package com.tonyodev.dispatch.internals

import com.tonyodev.dispatch.*
import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import com.tonyodev.dispatch.utils.*
import com.tonyodev.dispatch.utils.INVALID_RESULT
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.getNewDispatchId

internal class DispatchQueueImpl<T, R>(override var dispatchId: String,
                                       private val delayInMillis: Long = 0,
                                       private var worker: ((T) -> R)?,
                                       private val dispatchQueueInfo: DispatchQueueInfo,
                                       private val threadHandlerInfo: Threader.ThreadHandlerInfo): DispatchQueue<R> {

    private val dispatchSources = ArrayList<DispatchQueue<*>?>(3)
    private val dispatchObservable = DispatchObservable<R>(null, false)
    private var doOnErrorWorker: ((throwable: Throwable) -> R)? = null

    override val id: Int
        get() {
            return dispatchQueueInfo.queueId
        }

    override val isCancelled: Boolean
        get() {
            return dispatchQueueInfo.isCancelled
        }

    override val root: DispatchQueue<*>
        get() {
            return dispatchQueueInfo.rootDispatchQueue
        }

    override val controller: DispatchQueueController?
        get() {
            return dispatchQueueInfo.dispatchQueueController
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
                            result1 = (dispatchSources[0] as DispatchQueueImpl<*, *>).results
                            result2 = (dispatchSources[1] as DispatchQueueImpl<*, *>).results
                            result3 = (dispatchSources[2] as DispatchQueueImpl<*, *>).results
                            if (hasInvalidResult(result1, result2, result3)) {
                                INVALID_RESULT
                            } else {
                                Triple(result1, result2, result3)
                            }
                        }
                        2 -> {
                            result1 = (dispatchSources[0] as DispatchQueueImpl<*, *>).results
                            result2 = (dispatchSources[1] as DispatchQueueImpl<*, *>).results
                            if (hasInvalidResult(result1, result2)) {
                                INVALID_RESULT
                            } else {
                                Pair(result1, result2)
                            }
                        }
                        else -> {
                            result1 = (dispatchSources[0] as DispatchQueueImpl<*, *>).results
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
                    results = null
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
                dispatchQueueInfo.isIntervalDispatch -> dispatchQueueInfo.rootDispatchQueue.runDispatcher()
                else -> {
                    dispatchQueueInfo.completedDispatchQueue = true
                    if (!dispatchQueueInfo.isIntervalDispatch) {
                        cancel()
                    }
                }
            }
        }
    }

    private fun getNextDispatchInfo(after: DispatchQueue<*>): DispatchQueueImpl<*, *>? {
        return synchronized(dispatchQueueInfo.queue) {
            val iterator = dispatchQueueInfo.queue.iterator()
            var self: DispatchQueueImpl<*, *>? = null
            var nextDispatch: DispatchQueueImpl<*, *>? = null
            var dispatch: DispatchQueueImpl<*, *>
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
            if (dispatchQueueInfo.dispatchQueueController == null && Dispatcher.enableLogWarnings && this == dispatchQueueInfo.rootDispatchQueue) {
                Dispatcher.logger.print(
                    TAG, "No DispatchQueueController set for dispatch queue with id: $id. " +
                            "Not setting a DispatchQueueController can cause memory leaks for long running tasks.")
            }
            when {
                delayInMillis >= 1 -> threadHandlerInfo.threadHandler.postDelayed(delayInMillis, dispatcher)
                else -> threadHandlerInfo.threadHandler.post(dispatcher)
            }
        }
    }

    private fun handleException(throwable: Throwable) {
        val mainErrorHandler = dispatchQueueInfo.errorHandler
        if (mainErrorHandler != null) {
            Threader.getHandlerThreadInfo(ThreadType.MAIN)
                .threadHandler.post(Runnable { mainErrorHandler.invoke(throwable, this.dispatchId) })
            cancel()
            return
        }
        val globalHandler = Dispatcher.globalErrorHandler
        if (globalHandler != null) {
            Threader.getHandlerThreadInfo(ThreadType.MAIN)
                .threadHandler.post(Runnable { globalHandler.invoke(throwable, this, this.dispatchId) })
            cancel()
            return
        }
        cancel()
        throw throwable
    }

    override fun start(): DispatchQueue<R> {
        return start(null)
    }

    override fun start(errorHandler: ((throwable: Throwable, dispatchId: String) -> Unit)?): DispatchQueue<R> {
        if (!isCancelled) {
            dispatchQueueInfo.errorHandler = errorHandler
            dispatchQueueInfo.completedDispatchQueue = false
            dispatchQueueInfo.rootDispatchQueue.runDispatcher()
        } else {
            throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        }
        return this
    }

    override fun cancel(): DispatchQueue<R> {
        if (!isCancelled) {
            dispatchQueueInfo.isCancelled = true
            val dispatchQueueController = dispatchQueueInfo.dispatchQueueController
            dispatchQueueInfo.dispatchQueueController = null
            if (dispatchQueueController is LifecycleDispatchQueueController) {
                dispatchQueueController.unmanage(this)
            } else {
                dispatchQueueController?.unmanage(this)
            }
            synchronized(dispatchQueueInfo.queue) {
                val iterator = dispatchQueueInfo.queue.iterator()
                var dispatch: DispatchQueueImpl<*, *>?
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
                dispatchQueueInfo.errorHandler = null
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

    override fun doOnError(func: ((Throwable) -> R)): DispatchQueue<R> {
        this.doOnErrorWorker = func
        return this
    }

    override fun managedBy(dispatchQueueController: DispatchQueueController): DispatchQueue<R> {
        if (dispatchQueueController is LifecycleDispatchQueueController) {
            managedBy(dispatchQueueController, CancelType.DESTROYED)
        } else {
            val oldDispatchQueueController = this.dispatchQueueInfo.dispatchQueueController
            this.dispatchQueueInfo.dispatchQueueController = null
            oldDispatchQueueController?.unmanage(this)
            this.dispatchQueueInfo.dispatchQueueController = dispatchQueueController
            dispatchQueueController.manage(this)
        }
        return this
    }

    override fun managedBy(lifecycleDispatchQueueController: LifecycleDispatchQueueController, cancelType: CancelType): DispatchQueue<R> {
        val oldDispatchQueueController = this.dispatchQueueInfo.dispatchQueueController
        this.dispatchQueueInfo.dispatchQueueController = null
        oldDispatchQueueController?.unmanage(this)
        this.dispatchQueueInfo.dispatchQueueController = lifecycleDispatchQueueController
        lifecycleDispatchQueueController.manage(this, cancelType)
        return this
    }

    override fun <U> post(func: (R) -> U): DispatchQueue<U> {
        return post(0, func)
    }

    override fun <U> post(delayInMillis: Long, func: (R) -> U): DispatchQueue<U> {
        return getNewDispatchQueue(func, delayInMillis, Threader.getHandlerThreadInfo(ThreadType.MAIN))
    }

    override fun <U> async(func: (R) -> U): DispatchQueue<U> {
        return async(0, func)
    }

    override fun <U> async(delayInMillis: Long, func: (R) -> U): DispatchQueue<U> {
        return getNewDispatchQueue(func, delayInMillis, dispatchQueueInfo.threadHandlerInfo)
    }

    override fun <T> map(func: (R) -> T): DispatchQueue<T> {
        return async(func)
    }

    private fun <T, R> getNewDispatchQueue(worker: (T) -> R, delayInMillis: Long, threadHandlerInfo: Threader.ThreadHandlerInfo): DispatchQueue<R> {
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        return synchronized(dispatchQueueInfo.queue) {
            val newDispatchQueue = DispatchQueueImpl(
                dispatchId = getNewDispatchId(),
                delayInMillis = delayInMillis,
                worker = worker,
                dispatchQueueInfo = dispatchQueueInfo,
                threadHandlerInfo = threadHandlerInfo)
            if (!isCancelled) {
                newDispatchQueue.dispatchSources.add(this)
                throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
                dispatchQueueInfo.queue.add(newDispatchQueue)
                if (dispatchQueueInfo.completedDispatchQueue) {
                    dispatchQueueInfo.completedDispatchQueue = false
                    newDispatchQueue.runDispatcher()
                }
            } else {
                throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
            }
            newDispatchQueue
        }
    }

    override fun <U> zip(dispatchQueue: DispatchQueue<U>): DispatchQueue<Pair<R, U>> {
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        return synchronized(dispatchQueueInfo.queue) {
            val newDispatch = DispatchQueueImpl<Pair<R, U>, Pair<R, U>>(
                dispatchId = getNewDispatchId(),
                delayInMillis = 0,
                worker = { it },
                dispatchQueueInfo = dispatchQueueInfo,
                threadHandlerInfo = dispatchQueueInfo.threadHandlerInfo)
            if (!isCancelled) {
                newDispatch.dispatchSources.add(this)
                newDispatch.dispatchSources.add((dispatchQueue as DispatchQueueImpl<*, *>).cloneTo(newDispatchQueueInfo = dispatchQueueInfo))
                throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
                dispatchQueueInfo.queue.add(newDispatch)
                if (dispatchQueueInfo.completedDispatchQueue) {
                    dispatchQueueInfo.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
            } else {
                throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
            }
            newDispatch
        }
    }

    override fun <U, T> zip(dispatchQueue: DispatchQueue<U>, dispatchQueue2: DispatchQueue<T>): DispatchQueue<Triple<R, U, T>> {
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        return synchronized(dispatchQueueInfo.queue) {
            val newDispatchQueue = DispatchQueueImpl<Triple<R, U, T>, Triple<R, U, T>>(
                dispatchId = getNewDispatchId(),
                delayInMillis = 0,
                worker = { it },
                dispatchQueueInfo = dispatchQueueInfo,
                threadHandlerInfo = dispatchQueueInfo.threadHandlerInfo)
            if (!isCancelled) {
                newDispatchQueue.dispatchSources.add(this)
                newDispatchQueue.dispatchSources.add((dispatchQueue as DispatchQueueImpl<*, *>).cloneTo(newDispatchQueueInfo = dispatchQueueInfo))
                newDispatchQueue.dispatchSources.add((dispatchQueue2 as DispatchQueueImpl<*, *>).cloneTo(newDispatchQueueInfo = dispatchQueueInfo))
                throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
                dispatchQueueInfo.queue.add(newDispatchQueue)
                if (dispatchQueueInfo.completedDispatchQueue) {
                    dispatchQueueInfo.completedDispatchQueue = false
                    newDispatchQueue.runDispatcher()
                }
            } else {
                throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
            }
            newDispatchQueue
        }
    }

    private fun cloneTo(newDispatchQueueInfo: DispatchQueueInfo): DispatchQueueImpl<T, R> {
        val newDispatchQueue = DispatchQueueImpl(
            dispatchId = dispatchId,
            delayInMillis = delayInMillis,
            worker = worker,
            dispatchQueueInfo = newDispatchQueueInfo,
            threadHandlerInfo = newDispatchQueueInfo.threadHandlerInfo)
        newDispatchQueue.results = results
        newDispatchQueue.doOnErrorWorker = doOnErrorWorker
        newDispatchQueue.dispatchObservable.addObservers(dispatchObservable.getObservers())
        for (dispatchSource in dispatchSources) {
            val source = dispatchSource as DispatchQueueImpl<*, *>
            newDispatchQueue.dispatchSources.add(source.cloneTo(newDispatchQueueInfo = newDispatchQueueInfo))
        }
        synchronized(newDispatchQueueInfo.queue) {
            throwIllegalStateExceptionIfCancelled(newDispatchQueueInfo)
            newDispatchQueueInfo.queue.add(newDispatchQueue)
        }
        return newDispatchQueue
    }

    override fun addObserver(dispatchObserver: DispatchObserver<R>): DispatchQueue<R> {
        dispatchObservable.addObserver(dispatchObserver)
        return this
    }

    override fun addObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchQueue<R> {
        dispatchObservable.addObservers(dispatchObservers)
        return this
    }

    override fun removeObserver(dispatchObserver: DispatchObserver<R>): DispatchQueue<R> {
        dispatchObservable.removeObserver(dispatchObserver)
        return this
    }

    override fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchQueue<R> {
        dispatchObservable.removeObservers(dispatchObservers)
        return this
    }

    override fun removeObservers(): DispatchQueue<R> {
        dispatchObservable.removeObservers()
        return this
    }

    override fun getDispatchObservable(): DispatchObservable<R> {
        return dispatchObservable
    }

    override fun setDispatchId(dispatchId: String): DispatchQueue<R> {
        this.dispatchId = dispatchId
        return this
    }

    override fun toString(): String {
        return "DispatchQueue(dispatchId='$dispatchId', id='${dispatchQueueInfo.queueId}')"
    }

}