package com.tonyodev.dispatch.internals

import com.tonyodev.dispatch.*
import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import com.tonyodev.dispatch.utils.*
import com.tonyodev.dispatch.utils.INVALID_RESULT
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.getNewDispatchId

internal class DispatchQueueImpl<T, R>(override var blockLabel: String,
                                       private val delayInMillis: Long = 0,
                                       private var worker: ((T) -> R)?,
                                       private val dispatchQueueInfo: DispatchQueueInfo,
                                       private val threadHandlerInfo: Threader.ThreadHandlerInfo): DispatchQueue<R> {

    private val dispatchSources = ArrayList<DispatchQueue<*>?>(3)
    private val dispatchQueueObservable = DispatchQueueObservable<R>(null, false)
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

    private var dispatcher: Runnable? = null

    @Suppress("UNCHECKED_CAST")
    private fun getDispatcher(): Runnable {
        return Runnable {
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
                            processNextDispatchQueue()
                        }
                    } else {
                        results = null
                        notifyDispatchObservers()
                        processNextDispatchQueue()
                    }
                }
            } catch (err: Exception) {
                val doOnErrorWorker = doOnErrorWorker
                if (doOnErrorWorker != null && !isCancelled) {
                    try {
                        results = doOnErrorWorker.invoke(err)
                        notifyDispatchObservers()
                        processNextDispatchQueue()
                    } catch (e: Exception) {
                        handleException(e)
                    }
                } else {
                    handleException(err)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun notifyDispatchObservers() {
        if (!isCancelled) {
            dispatchQueueObservable.notify(results as R)
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

    private fun processNextDispatchQueue() {
        if (!isCancelled) {
            val nextDispatchQueue = getNextDispatchQueueInfo(this)
            when {
                nextDispatchQueue != null -> nextDispatchQueue.runDispatcher()
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

    private fun getNextDispatchQueueInfo(after: DispatchQueue<*>): DispatchQueueImpl<*, *>? {
        return synchronized(dispatchQueueInfo.queue) {
            var nextDispatchQueue: DispatchQueueImpl<*, *>? = null
            var index = dispatchQueueInfo.queue.indexOf(after)
            if (index > -1) {
               index += 1
                if (index < dispatchQueueInfo.queue.size) {
                    nextDispatchQueue = dispatchQueueInfo.queue[index]
                }
            }
            if (isCancelled) {
                nextDispatchQueue = null
            }
            nextDispatchQueue
        }
    }

    private fun runDispatcher() {
        if (!isCancelled) {
            var dispatcherRunnable = dispatcher
            if (dispatcherRunnable != null) {
                threadHandlerInfo.threadHandler.removeCallbacks(dispatcherRunnable)
                dispatcher = null
            }
            dispatcherRunnable = getDispatcher()
            dispatcher = dispatcherRunnable
            if (dispatchQueueInfo.dispatchQueueController == null && DispatchQueue.globalSettings.enableLogWarnings && this == dispatchQueueInfo.rootDispatchQueue) {
                DispatchQueue.globalSettings.logger.print(
                    TAG, "No DispatchQueueController set for dispatch queue with id: $id. " +
                            "Not setting a DispatchQueueController can cause memory leaks for long running tasks.")
            }
            when {
                delayInMillis >= 1 -> threadHandlerInfo.threadHandler.postDelayed(delayInMillis, dispatcherRunnable)
                else -> threadHandlerInfo.threadHandler.post(dispatcherRunnable)
            }
        }
    }

    private fun handleException(throwable: Throwable) {
        val mainErrorHandler = dispatchQueueInfo.dispatchQueueErrorCallback
        if (mainErrorHandler != null) {
            Threader.getHandlerThreadInfo(ThreadType.MAIN)
                .threadHandler.post(Runnable { mainErrorHandler.onError(DispatchQueueError(throwable, this, this.blockLabel)) })
            cancel()
            return
        }
        val globalHandler = DispatchQueue.globalSettings.dispatchQueueErrorCallback
        if (globalHandler != null) {
            Threader.getHandlerThreadInfo(ThreadType.MAIN)
                .threadHandler.post(Runnable { globalHandler.onError(DispatchQueueError(throwable, this, this.blockLabel)) })
            cancel()
            return
        }
        cancel()
        throw throwable
    }

    override fun start(): DispatchQueue<R> {
        return start(null)
    }

    override fun start(dispatchQueueErrorCallback: DispatchQueueErrorCallback?): DispatchQueue<R> {
        if (!isCancelled) {
            if (!dispatchQueueInfo.isStarted) {
                dispatchQueueInfo.isStarted = true
                dispatchQueueInfo.dispatchQueueErrorCallback = dispatchQueueErrorCallback
                dispatchQueueInfo.completedDispatchQueue = false
                dispatchQueueInfo.rootDispatchQueue.runDispatcher()
            }
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
                var dispatchQueue: DispatchQueueImpl<*, *>?
                var sourceIterator: Iterator<*>
                while (iterator.hasNext()) {
                    dispatchQueue = iterator.next()
                    dispatchQueue.removeDispatcher()
                    sourceIterator = dispatchQueue.dispatchSources.iterator()
                    while (sourceIterator.hasNext()) {
                        sourceIterator.next()
                        sourceIterator.remove()
                    }
                    iterator.remove()
                }
                dispatchQueueObservable.removeObservers()
                dispatchQueueObservable.resetResult()
                results = INVALID_RESULT
                dispatcher = null
                doOnErrorWorker = null
                worker = null
                dispatchQueueInfo.isStarted = false
                dispatchQueueInfo.dispatchQueueErrorCallback = null
            }
        }
        return this
    }

    private fun removeDispatcher() {
        val dispatcherRunnable = dispatcher
        dispatcher = null
        if (dispatcherRunnable != null) {
            threadHandlerInfo.threadHandler.removeCallbacks(dispatcherRunnable)
        }
        if (threadHandlerInfo.closeThreadHandler && threadHandlerInfo.threadHandler.isActive) {
            threadHandlerInfo.threadHandler.quit()
        }
    }

    override fun doOnError(func: ((Throwable) -> R)): DispatchQueue<R> {
        this.doOnErrorWorker = func
        return this
    }

    override fun managedBy(dispatchQueueController: DispatchQueueController?): DispatchQueue<R> {
        if (dispatchQueueController is LifecycleDispatchQueueController) {
            managedBy(dispatchQueueController, CancelType.DESTROYED)
        } else {
            val oldDispatchQueueController = this.dispatchQueueInfo.dispatchQueueController
            this.dispatchQueueInfo.dispatchQueueController = null
            oldDispatchQueueController?.unmanage(this)
            this.dispatchQueueInfo.dispatchQueueController = dispatchQueueController
            dispatchQueueController?.manage(this)
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
                blockLabel = getNewDispatchId(),
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
                    runDispatcher()
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
                blockLabel = getNewDispatchId(),
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
                    runDispatcher()
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
                blockLabel = getNewDispatchId(),
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
                    runDispatcher()
                }
            } else {
                throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
            }
            newDispatchQueue
        }
    }

    private fun cloneTo(newDispatchQueueInfo: DispatchQueueInfo): DispatchQueueImpl<T, R> {
        val newDispatchQueue = DispatchQueueImpl(
            blockLabel = blockLabel,
            delayInMillis = delayInMillis,
            worker = worker,
            dispatchQueueInfo = newDispatchQueueInfo,
            threadHandlerInfo = newDispatchQueueInfo.threadHandlerInfo)
        newDispatchQueue.results = results
        newDispatchQueue.doOnErrorWorker = doOnErrorWorker
        newDispatchQueue.dispatchQueueObservable.addObservers(dispatchQueueObservable.getObservers())
        synchronized(dispatchSources) {
            for (dispatchSource in dispatchSources) {
                val source = dispatchSource as DispatchQueueImpl<*, *>
                newDispatchQueue.dispatchSources.add(source.cloneTo(newDispatchQueueInfo = newDispatchQueueInfo))
            }
        }
        synchronized(newDispatchQueueInfo.queue) {
            throwIllegalStateExceptionIfCancelled(newDispatchQueueInfo)
            newDispatchQueueInfo.queue.add(newDispatchQueue)
        }
        return newDispatchQueue
    }

    override fun addObserver(dispatchQueueObserver: DispatchQueueObserver<R>): DispatchQueue<R> {
        dispatchQueueObservable.addObserver(dispatchQueueObserver)
        return this
    }

    override fun addObservers(dispatchQueueObservers: Collection<DispatchQueueObserver<R>>): DispatchQueue<R> {
        dispatchQueueObservable.addObservers(dispatchQueueObservers)
        return this
    }

    override fun removeObserver(dispatchQueueObserver: DispatchQueueObserver<R>): DispatchQueue<R> {
        dispatchQueueObservable.removeObserver(dispatchQueueObserver)
        return this
    }

    override fun removeObservers(dispatchQueueObservers: Collection<DispatchQueueObserver<R>>): DispatchQueue<R> {
        dispatchQueueObservable.removeObservers(dispatchQueueObservers)
        return this
    }

    override fun removeObservers(): DispatchQueue<R> {
        dispatchQueueObservable.removeObservers()
        return this
    }

    override fun getDispatchQueueObservable(): DispatchQueueObservable<R> {
        return dispatchQueueObservable
    }

    override fun setBlockLabel(blockLabel: String): DispatchQueue<R> {
        this.blockLabel = blockLabel
        return this
    }

    override fun toString(): String {
        return "DispatchQueue(blockLabel='$blockLabel', id='${dispatchQueueInfo.queueId}')"
    }

}