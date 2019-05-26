package com.tonyodev.dispatch.internals

import com.tonyodev.dispatch.*
import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import com.tonyodev.dispatch.utils.*
import com.tonyodev.dispatch.utils.INVALID_RESULT
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.getNewDispatchId
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

internal class DispatchQueueImpl<T, R>(override var blockLabel: String,
                                       private val delayInMillis: Long = 0,
                                       private var worker: ((T) -> R)?,
                                       private val dispatchQueueInfo: DispatchQueueInfo,
                                       private val threadHandlerInfo: Threader.ThreadHandlerInfo): DispatchQueue<R> {

    private val dispatchSources = arrayOfNulls<DispatchQueueImpl<*, *>>(3)
    private var sourceCount = 0
    private val dispatchQueueObservable = DispatchQueueObservable<R>(null)
    private var doOnErrorWorker: ((throwable: Throwable) -> R)? = null
    private var retryCount = 0
    private var retryAttempt = 0
    private var retryDelayInMillis = 0L
    private var next: DispatchQueueImpl<*, *>? = null
    private var results: Any? = INVALID_RESULT
    private var dispatcher: Runnable? = null

    override val id: Int
        get() {
            return dispatchQueueInfo.queueId
        }

    override val isCancelled: Boolean
        get() {
            return dispatchQueueInfo.isCancelled
        }

    override val controller: DispatchQueueController?
        get() {
            return dispatchQueueInfo.dispatchQueueController
        }

    @Suppress("UNCHECKED_CAST")
    private fun getDispatcher(): Runnable {
        return Runnable {
            try {
                if (!isCancelled) {
                    if (worker != null && sourceCount > 0) {
                        val result1: Any?
                        val result2: Any?
                        val result3: Any?
                        val data = when (sourceCount) {
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
                if (retryAttempt < retryCount) {
                    runDispatcher(true)
                } else {
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
            val nextDispatchQueue = next
            when {
                nextDispatchQueue != null -> nextDispatchQueue.runDispatcher()
                dispatchQueueInfo.isIntervalDispatch -> dispatchQueueInfo.rootDispatchQueue?.runDispatcher()
                else -> {
                    if (!dispatchQueueInfo.isIntervalDispatch) {
                        cancel()
                    }
                }
            }
        }
    }

    private fun runDispatcher(isRetryAttempt: Boolean = false) {
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
                isRetryAttempt -> {
                    retryAttempt += 1
                    threadHandlerInfo.threadHandler.postDelayed(retryDelayInMillis, dispatcherRunnable)
                }
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

    private fun enqueue(dispatchQueueImpl: DispatchQueueImpl<*, *>) {
        synchronized(dispatchQueueInfo) {
            if (dispatchQueueInfo.canPerformOperations()) {
                dispatchQueueInfo.endDispatchQueue?.next = dispatchQueueImpl
                dispatchQueueInfo.endDispatchQueue = dispatchQueueImpl
            }
        }
    }

    private fun addSource(source: DispatchQueueImpl<*, *>) {
        if (dispatchQueueInfo.canPerformOperations() && sourceCount < 3) {
            dispatchSources[sourceCount] = source
            sourceCount += 1
        }
    }

    override fun start(): DispatchQueue<R> {
        return start(null)
    }

    override fun start(dispatchQueueErrorCallback: DispatchQueueErrorCallback?): DispatchQueue<R> {
        if (!isCancelled) {
            if (!dispatchQueueInfo.isStarted) {
                dispatchQueueInfo.isStarted = true
                dispatchQueueInfo.dispatchQueueErrorCallback = dispatchQueueErrorCallback
                dispatchQueueInfo.rootDispatchQueue?.runDispatcher()
                DispatchQueue.globalSettings.logger.print(TAG, "DispatchQueue with id ${dispatchQueueInfo.queueId} has started.")
            }
        } else {
            throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        }
        return this
    }

    override fun cancel(): DispatchQueue<R> {
        if (!isCancelled) {
            dispatchQueueInfo.isCancelled = true
            dispatchQueueInfo.isStarted = false
            dispatchQueueInfo.dispatchQueueErrorCallback = null
            val dispatchQueueController = dispatchQueueInfo.dispatchQueueController
            dispatchQueueInfo.dispatchQueueController = null
            if (dispatchQueueController is LifecycleDispatchQueueController) {
                dispatchQueueController.unmanage(this)
            } else {
                dispatchQueueController?.unmanage(this)
            }
            var current = dispatchQueueInfo.rootDispatchQueue
            var next: DispatchQueueImpl<*, *>?
            var sourceSize: Int
            while (current != null) {
                current.removeDispatcher()
                sourceSize = current.sourceCount
                current.sourceCount = 0
                for (index in 0 until sourceSize) {
                    current.dispatchSources[index] = null
                }
                current.dispatchQueueObservable.removeObservers()
                current.results = INVALID_RESULT
                current.dispatcher = null
                current.doOnErrorWorker = null
                current.worker = null
                current.retryAttempt = 0
                next = current.next
                current.next = null
                current = next
            }
            dispatchQueueInfo.rootDispatchQueue = null
            dispatchQueueInfo.endDispatchQueue = null
            DispatchQueue.globalSettings.logger.print(TAG, "DispatchQueue with id ${dispatchQueueInfo.queueId} has been cancelled.")
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
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
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
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        val newDispatchQueue = DispatchQueueImpl(
            blockLabel = getNewDispatchId(),
            delayInMillis = delayInMillis,
            worker = worker,
            dispatchQueueInfo = dispatchQueueInfo,
            threadHandlerInfo = threadHandlerInfo)
        newDispatchQueue.addSource(this)
        enqueue(newDispatchQueue)
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        return newDispatchQueue
    }

    override fun <U> zip(dispatchQueue: DispatchQueue<U>): DispatchQueue<Pair<R, U>> {
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        val newDispatchQueue = DispatchQueueImpl<Pair<R, U>, Pair<R, U>>(
            blockLabel = getNewDispatchId(),
            delayInMillis = 0,
            worker = { it },
            dispatchQueueInfo = dispatchQueueInfo,
            threadHandlerInfo = dispatchQueueInfo.threadHandlerInfo)
        newDispatchQueue.addSource(this)
        newDispatchQueue.addSource((dispatchQueue as DispatchQueueImpl<*, *>).cloneTo(newDispatchQueueInfo = dispatchQueueInfo))
        enqueue(newDispatchQueue)
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        return newDispatchQueue
    }

    override fun <U, T> zip(dispatchQueue: DispatchQueue<U>, dispatchQueue2: DispatchQueue<T>): DispatchQueue<Triple<R, U, T>> {
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        val newDispatchQueue = DispatchQueueImpl<Triple<R, U, T>, Triple<R, U, T>>(
            blockLabel = getNewDispatchId(),
            delayInMillis = 0,
            worker = { it },
            dispatchQueueInfo = dispatchQueueInfo,
            threadHandlerInfo = dispatchQueueInfo.threadHandlerInfo)
        newDispatchQueue.addSource(this)
        newDispatchQueue.addSource((dispatchQueue as DispatchQueueImpl<*, *>).cloneTo(newDispatchQueueInfo = dispatchQueueInfo))
        newDispatchQueue.addSource((dispatchQueue2 as DispatchQueueImpl<*, *>).cloneTo(newDispatchQueueInfo = dispatchQueueInfo))
        enqueue(newDispatchQueue)
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        return newDispatchQueue
    }

    private fun cloneTo(newDispatchQueueInfo: DispatchQueueInfo): DispatchQueueImpl<T, R> {
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        val newDispatchQueue = DispatchQueueImpl(
            blockLabel = blockLabel,
            delayInMillis = delayInMillis,
            worker = worker,
            dispatchQueueInfo = newDispatchQueueInfo,
            threadHandlerInfo = newDispatchQueueInfo.threadHandlerInfo)
        newDispatchQueue.results = results
        newDispatchQueue.doOnErrorWorker = doOnErrorWorker
        newDispatchQueue.retryCount = retryCount
        newDispatchQueue.retryAttempt = retryAttempt
        newDispatchQueue.retryDelayInMillis = retryDelayInMillis
        newDispatchQueue.dispatchQueueObservable.addObservers(dispatchQueueObservable.getObservers())
        var source: DispatchQueueImpl<*, *>?
        for (dispatchSource in dispatchSources) {
            source = dispatchSource
            if (source != null) {
                newDispatchQueue.addSource(source.cloneTo(newDispatchQueueInfo = newDispatchQueueInfo))
            }
        }
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        newDispatchQueueInfo.endDispatchQueue?.enqueue(newDispatchQueue)
        throwIllegalStateExceptionIfStarted(newDispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(newDispatchQueueInfo)
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

    override fun retry(count: Int, delayInMillis: Long): DispatchQueue<R> {
        throwIllegalStateExceptionIfStarted(dispatchQueueInfo)
        throwIllegalStateExceptionIfCancelled(dispatchQueueInfo)
        if (count < 0) {
            throw IllegalArgumentException("Count cannot be less than zero")
        }
        if (delayInMillis < 0) {
            throw IllegalArgumentException("Delay cannot be less than zero")
        }
        this.retryCount = count
        this.retryDelayInMillis = delayInMillis
        return this
    }

    override fun retry(count: Int): DispatchQueue<R> {
        return retry(count, 0)
    }

    override fun toString(): String {
        return "DispatchQueue(blockLabel='$blockLabel', id='${dispatchQueueInfo.queueId}')"
    }

}