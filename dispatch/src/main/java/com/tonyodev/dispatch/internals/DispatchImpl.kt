package com.tonyodev.dispatch.internals

import com.tonyodev.dispatch.*
import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import com.tonyodev.dispatch.thread.ThreadHandler
import com.tonyodev.dispatch.utils.*
import com.tonyodev.dispatch.utils.INVALID_RESULT
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.getNewDispatchId
import com.tonyodev.dispatch.utils.startThreadHandlerIfNotActive
import com.tonyodev.dispatch.utils.throwIfUsesMainThreadForBackgroundWork

internal class DispatchImpl<T, R>(override var dispatchId: String,
                                  private val delayInMillis: Long = 0,
                                  private var worker: ((T) -> R)?,
                                  private val dispatchQueue: DispatchQueue,
                                  private val threadHandlerInfo: Threader.ThreadHandlerInfo): Dispatch<R> {

    private val dispatchSources = ArrayList<Dispatch<*>?>(3)
    private val dispatchObservable = DispatchObservable<R>(null, false)
    private var doOnErrorWorker: ((throwable: Throwable) -> R)? = null

    override val queueId: Int
        get() {
            return dispatchQueue.queueId
        }

    override val isCancelled: Boolean
        get() {
            return dispatchQueue.isCancelled
        }

    override val rootDispatch: Dispatch<*>
        get() {
            return dispatchQueue.rootDispatch
        }

    override val dispatchQueueController: DispatchQueueController?
        get() {
            return dispatchQueue.dispatchQueueController
        }

    private var results: Any? = INVALID_RESULT

    @Suppress("UNCHECKED_CAST")
    private val dispatcher = Runnable {
        try {
            if (!isCancelled) {
                if (worker != null && dispatchSources.isNotEmpty()) {
                    val result1: Any?
                    val result2: Any?
                    val result3: Any?
                    val data = when (dispatchSources.size) {
                        3 -> {
                            result1 = (dispatchSources[0] as DispatchImpl<*, *>).results
                            result2 = (dispatchSources[1] as DispatchImpl<*, *>).results
                            result3 = (dispatchSources[2] as DispatchImpl<*, *>).results
                            if (hasInvalidResult(result1, result2, result3)) {
                                INVALID_RESULT
                            } else {
                                Triple(result1, result2, result3)
                            }
                        }
                        2 -> {
                            result1 = (dispatchSources[0] as DispatchImpl<*, *>).results
                            result2 = (dispatchSources[1] as DispatchImpl<*, *>).results
                            if (hasInvalidResult(result1, result2)) {
                                INVALID_RESULT
                            } else {
                                Pair(result1, result2)
                            }
                        }
                        else -> {
                            result1 = (dispatchSources[0] as DispatchImpl<*, *>).results
                            if (hasInvalidResult(result1)) {
                                INVALID_RESULT
                            } else {
                                result1
                            }
                        }
                    }
                    if (data != INVALID_RESULT && !isCancelled) {
                        val func = worker
                        if (func != null) {
                            results = func.invoke(data as T)
                        } else {
                            results = INVALID_RESULT
                        }
                        notifyDispatchObservers()
                        processNextDispatch()
                    }
                } else {
                    results = Unit
                    notifyDispatchObservers()
                    processNextDispatch()
                }
            }
        } catch (err: Exception) {
            val doOnErrorWorker = doOnErrorWorker
            if (doOnErrorWorker != null && !isCancelled) {
                try {
                    results = doOnErrorWorker.invoke(err)
                    notifyDispatchObservers()
                    processNextDispatch()
                } catch (e: Exception) {
                    handleException(e)
                }
            } else {
                handleException(err)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun notifyDispatchObservers() {
        if (!isCancelled) {
            dispatchObservable.notify(results as R)
        }
    }

    private fun hasInvalidResult(vararg results: Any?): Boolean {
        for (result in results) {
            if (result == INVALID_RESULT) {
                return true
            }
        }
        return false
    }

    private fun processNextDispatch() {
        if (!isCancelled) {
            val nextDispatch = getNextDispatchInfo(this)
            when {
                nextDispatch != null -> nextDispatch.runDispatcher()
                dispatchQueue.isIntervalDispatch -> dispatchQueue.rootDispatch.runDispatcher()
                else -> {
                    dispatchQueue.completedDispatchQueue = true
                    if (!dispatchQueue.isIntervalDispatch && dispatchQueue.cancelOnComplete) {
                        cancel()
                    }
                }
            }
        }
    }

    private fun getNextDispatchInfo(after: Dispatch<*>): DispatchImpl<*, *>? {
        return synchronized(dispatchQueue.queue) {
            val iterator = dispatchQueue.queue.iterator()
            var self: DispatchImpl<*, *>? = null
            var nextDispatch: DispatchImpl<*, *>? = null
            var dispatch: DispatchImpl<*, *>
            while (iterator.hasNext() && !isCancelled) {
                dispatch = iterator.next()
                if (self != null) {
                    nextDispatch = dispatch
                    break
                }
                if (dispatch == after) {
                    self = dispatch
                }
            }
            if (isCancelled) {
                nextDispatch = null
            }
            nextDispatch
        }
    }

    private fun runDispatcher() {
        if (!isCancelled) {
            threadHandlerInfo.threadHandler.removeCallbacks(dispatcher)
            if (dispatchQueue.dispatchQueueController == null && Dispatcher.enableLogWarnings && this == dispatchQueue.rootDispatch) {
                Dispatcher.logger.print(
                    TAG, "No DispatchQueueController set for dispatch queue with id: $queueId. " +
                            "Not setting a DispatchQueueController can cause memory leaks for long running tasks.")
            }
            when {
                delayInMillis >= 1 -> threadHandlerInfo.threadHandler.postDelayed(delayInMillis, dispatcher)
                else -> threadHandlerInfo.threadHandler.post(dispatcher)
            }
        }
    }

    private fun handleException(throwable: Throwable) {
        val mainErrorHandler = dispatchQueue.errorHandler
        if (mainErrorHandler != null) {
            Threader.getHandlerThreadInfo(ThreadType.MAIN)
                .threadHandler.post(Runnable { mainErrorHandler.invoke(throwable, this) })
            cancel()
            return
        }
        val globalHandler = Dispatcher.globalErrorHandler
        if (globalHandler != null) {
            Threader.getHandlerThreadInfo(ThreadType.MAIN)
                .threadHandler.post(Runnable { globalHandler.invoke(throwable, this) })
            cancel()
            return
        }
        cancel()
        throw throwable
    }

    override fun start(): Dispatch<R> {
        return start(null)
    }

    override fun start(errorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)?): Dispatch<R> {
        if (!isCancelled) {
            dispatchQueue.errorHandler = errorHandler
            dispatchQueue.completedDispatchQueue = false
            dispatchQueue.rootDispatch.runDispatcher()
        } else {
            throwIllegalStateExceptionIfCancelled(dispatchQueue)
        }
        return this
    }

    override fun cancel(): Dispatch<R> {
        if (!isCancelled) {
            dispatchQueue.isCancelled = true
            val dispatchQueueController = dispatchQueue.dispatchQueueController
            dispatchQueue.dispatchQueueController = null
            if (dispatchQueueController is LifecycleDispatchQueueController) {
                dispatchQueueController.unmanage(this)
            } else {
                dispatchQueueController?.unmanage(this)
            }
            synchronized(dispatchQueue.queue) {
                val iterator = dispatchQueue.queue.iterator()
                var dispatch: DispatchImpl<*, *>?
                var sourceIterator: Iterator<*>
                while (iterator.hasNext()) {
                    dispatch = iterator.next()
                    dispatch.removeDispatcher()
                    sourceIterator = dispatch.dispatchSources.iterator()
                    while (sourceIterator.hasNext()) {
                        sourceIterator.next()
                        sourceIterator.remove()
                    }
                    iterator.remove()
                }
                dispatchObservable.removeObservers()
                doOnErrorWorker = null
                worker = null
                dispatchQueue.errorHandler = null
            }
        }
        return this
    }

    private fun removeDispatcher() {
        threadHandlerInfo.threadHandler.removeCallbacks(dispatcher)
        if (threadHandlerInfo.closeHandler) {
            threadHandlerInfo.threadHandler.quit()
        }
    }

    override fun doOnError(func: ((Throwable) -> R)): Dispatch<R> {
        this.doOnErrorWorker = func
        return this
    }

    override fun managedBy(dispatchQueueController: DispatchQueueController): Dispatch<R> {
        val oldDispatchQueueController = this.dispatchQueue.dispatchQueueController
        this.dispatchQueue.dispatchQueueController = null
        oldDispatchQueueController?.unmanage(this)
        this.dispatchQueue.dispatchQueueController = dispatchQueueController
        dispatchQueueController.manage(this)
        dispatchQueue.cancelOnComplete = false
        return this
    }

    override fun managedBy(lifecycleDispatchQueueController: LifecycleDispatchQueueController): Dispatch<R> {
        return managedBy(lifecycleDispatchQueueController, CancelType.DESTROYED)
    }

    override fun managedBy(lifecycleDispatchQueueController: LifecycleDispatchQueueController, cancelType: CancelType): Dispatch<R> {
        val oldDispatchQueueController = this.dispatchQueue.dispatchQueueController
        this.dispatchQueue.dispatchQueueController = null
        oldDispatchQueueController?.unmanage(this)
        this.dispatchQueue.dispatchQueueController = lifecycleDispatchQueueController
        lifecycleDispatchQueueController.manage(this, cancelType)
        dispatchQueue.cancelOnComplete = false
        return this
    }

    override fun <U> post(func: (R) -> U): Dispatch<U> {
        return post(0, func)
    }

    override fun <U> post(delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        return getNewDispatch(func, delayInMillis, Threader.getHandlerThreadInfo(ThreadType.MAIN))
    }

    override fun <U> async(func: (R) -> U): Dispatch<U> {
        return async(0, func)
    }

    override fun <U> async(backgroundHandler: ThreadHandler, func: (R) -> U): Dispatch<U> {
        return async(backgroundHandler, 0, func)
    }

    override fun <U> async(delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        val workThreadHandlerInfo = when {
            threadHandlerInfo.threadName == Threader.getHandlerThreadInfo(ThreadType.MAIN).threadName -> Threader.getHandlerThreadInfo(ThreadType.BACKGROUND)
            else -> threadHandlerInfo
        }
        return getNewDispatch(func, delayInMillis, workThreadHandlerInfo)
    }

    override fun <U> async(backgroundHandler: ThreadHandler, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        startThreadHandlerIfNotActive(backgroundHandler)
        return getNewDispatch(func, delayInMillis, Threader.ThreadHandlerInfo(backgroundHandler, false))
    }

    override fun <U> async(threadType: ThreadType, func: (R) -> U): Dispatch<U> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        return getNewDispatch(func,0, Threader.getHandlerThreadInfo(threadType))
    }

    override fun <U> async(threadType: ThreadType, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        return getNewDispatch(func, delayInMillis, Threader.getHandlerThreadInfo(threadType))
    }

    override fun <T> map(func: (R) -> T): Dispatch<T> {
        return async(func)
    }

    private fun <T, R> getNewDispatch(worker: (T) -> R, delayInMillis: Long, threadHandlerInfo: Threader.ThreadHandlerInfo): Dispatch<R> {
        throwIllegalStateExceptionIfCancelled(dispatchQueue)
        return synchronized(dispatchQueue.queue) {
            val newDispatch = DispatchImpl(
                dispatchId = getNewDispatchId(),
                delayInMillis = delayInMillis,
                worker = worker,
                dispatchQueue = dispatchQueue,
                threadHandlerInfo = threadHandlerInfo)
            if (!isCancelled) {
                newDispatch.dispatchSources.add(this)
                throwIllegalStateExceptionIfCancelled(dispatchQueue)
                dispatchQueue.queue.add(newDispatch)
                if (dispatchQueue.completedDispatchQueue) {
                    dispatchQueue.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
            } else {
                throwIllegalStateExceptionIfCancelled(dispatchQueue)
            }
            newDispatch
        }
    }

    override fun <U> zip(dispatch: Dispatch<U>): Dispatch<Pair<R, U>> {
        throwIllegalStateExceptionIfCancelled(dispatchQueue)
        return synchronized(dispatchQueue.queue) {
            val newDispatch = DispatchImpl<Pair<R, U>, Pair<R, U>>(
                dispatchId = getNewDispatchId(),
                delayInMillis = 0,
                worker = { it },
                dispatchQueue = dispatchQueue,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND))
            if (!isCancelled) {
                newDispatch.dispatchSources.add(this)
                newDispatch.dispatchSources.add((dispatch as DispatchImpl<*, *>).cloneTo(newDispatchQueue = dispatchQueue))
                throwIllegalStateExceptionIfCancelled(dispatchQueue)
                dispatchQueue.queue.add(newDispatch)
                if (dispatchQueue.completedDispatchQueue) {
                    dispatchQueue.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
            } else {
                throwIllegalStateExceptionIfCancelled(dispatchQueue)
            }
            newDispatch
        }
    }

    override fun <U, T> zip(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R, U, T>> {
        throwIllegalStateExceptionIfCancelled(dispatchQueue)
        return synchronized(dispatchQueue.queue) {
            val newDispatch = DispatchImpl<Triple<R, U, T>, Triple<R, U, T>>(
                dispatchId = getNewDispatchId(),
                delayInMillis = 0,
                worker = { it },
                dispatchQueue = dispatchQueue,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND))
            if (!isCancelled) {
                newDispatch.dispatchSources.add(this)
                newDispatch.dispatchSources.add((dispatch as DispatchImpl<*, *>).cloneTo(newDispatchQueue = dispatchQueue))
                newDispatch.dispatchSources.add((dispatch2 as DispatchImpl<*, *>).cloneTo(newDispatchQueue = dispatchQueue))
                throwIllegalStateExceptionIfCancelled(dispatchQueue)
                dispatchQueue.queue.add(newDispatch)
                if (dispatchQueue.completedDispatchQueue) {
                    dispatchQueue.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
            } else {
                throwIllegalStateExceptionIfCancelled(dispatchQueue)
            }
            newDispatch
        }
    }

    private fun cloneTo(newDispatchQueue: DispatchQueue): DispatchImpl<T, R> {
        throwIllegalStateExceptionIfCancelled(this.dispatchQueue)
        val newDispatch = DispatchImpl(
            dispatchId = dispatchId,
            delayInMillis = delayInMillis,
            worker = worker,
            dispatchQueue = newDispatchQueue,
            threadHandlerInfo = threadHandlerInfo)
        newDispatch.results = results
        newDispatch.doOnErrorWorker = doOnErrorWorker
        newDispatch.dispatchObservable.addObservers(dispatchObservable.getObservers())
        for (dispatchSource in dispatchSources) {
            val source = dispatchSource as DispatchImpl<*, *>
            newDispatch.dispatchSources.add(source.cloneTo(newDispatchQueue = newDispatchQueue))
        }
        synchronized(newDispatchQueue.queue) {
            throwIllegalStateExceptionIfCancelled(newDispatchQueue)
            newDispatchQueue.queue.add(newDispatch)
        }
        return newDispatch
    }

    override fun cancelOnComplete(cancel: Boolean): Dispatch<R> {
        if (dispatchQueue.dispatchQueueController == null) {
            dispatchQueue.cancelOnComplete = cancel
            if (cancel && dispatchQueue.completedDispatchQueue) {
                cancel()
            }
        }
        return this
    }

    override fun addObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R> {
        dispatchObservable.addObserver(dispatchObserver)
        return this
    }

    override fun addObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R> {
        dispatchObservable.addObservers(dispatchObservers)
        return this
    }

    override fun removeObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R> {
        dispatchObservable.removeObserver(dispatchObserver)
        return this
    }

    override fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R> {
        dispatchObservable.removeObservers(dispatchObservers)
        return this
    }

    override fun removeObservers(): Dispatch<R> {
        dispatchObservable.removeObservers()
        return this
    }

    override fun getDispatchObservable(): DispatchObservable<R> {
        return dispatchObservable
    }

    override fun setDispatchId(id: String): Dispatch<R> {
        this.dispatchId = id
        return this
    }

    override fun toString(): String {
        return "Dispatch(dispatchId='$dispatchId', queueId='${dispatchQueue.queueId}')"
    }

}