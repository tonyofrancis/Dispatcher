package com.tonyodev.dispatch

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.util.Log

internal class DispatchImpl<T, R>(override var dispatchId: String,
                                  private val handler: Handler,
                                  private val delayInMillis: Long = 0,
                                  private val worker: ((T) -> R)?,
                                  private val closeHandler: Boolean,
                                  private val dispatchQueue: DispatchQueue,
                                  private val dispatchType: Int): Dispatch<R> {

    private val dispatchSources = ArrayList<Dispatch<*>?>(3)
    private val dispatchObservable = DispatchObservable.create<R>(handler, false)
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

    var results: Any? = INVALID_RESULT

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
                            if (dispatchType == DISPATCH_TYPE_ANY_RESULT) {
                                if ((dispatchSources[1] as DispatchImpl<*, *>).results == INVALID_RESULT
                                    && (dispatchSources[2] as DispatchImpl<*, *>).results == INVALID_RESULT) {
                                    return@Runnable
                                }
                            }
                            result1 = getSourceResult((dispatchSources[0] as DispatchImpl<*, *>))
                            result2 = getSourceResult((dispatchSources[1] as DispatchImpl<*, *>))
                            result3 = getSourceResult((dispatchSources[2] as DispatchImpl<*, *>))
                            if (hasInvalidResult(result1, result2, result3)) {
                                INVALID_RESULT
                            } else {
                                Triple(result1, result2, result3)
                            }
                        }
                        2 -> {
                            result1 = getSourceResult((dispatchSources[0] as DispatchImpl<*, *>))
                            result2 = getSourceResult((dispatchSources[1] as DispatchImpl<*, *>))
                            if (hasInvalidResult(result1, result2)) {
                                INVALID_RESULT
                            } else {
                                Pair(result1, result2)
                            }
                        }
                        else -> {
                            result1 = getSourceResult((dispatchSources[0] as DispatchImpl<*, *>))
                            if (hasInvalidResult(result1)) {
                                INVALID_RESULT
                            } else {
                                result1
                            }
                        }
                    }
                    if (data != INVALID_RESULT && !isCancelled) {
                        results = worker.invoke(data as T)
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

    private fun getSourceResult(sourceDispatchImpl: DispatchImpl<*, *>): Any? {
        var result = sourceDispatchImpl.results
        if (dispatchType == DISPATCH_TYPE_ANY_RESULT && result == INVALID_RESULT) {
            result = null
        }
        return result
    }

    private fun hasInvalidResult(vararg results: Any?): Boolean {
        if (dispatchType == DISPATCH_TYPE_ANY_RESULT) {
            return false
        }
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
        return nextDispatch
    }

    private fun runDispatcher() {
        if (!isCancelled) {
            handler.removeCallbacksAndMessages(dispatcher)
            if (delayInMillis >= 1) {
                handler.postDelayed(dispatcher, delayInMillis)
            } else {
                handler.post(dispatcher)
            }
            if (dispatchQueue.dispatchQueueController == null && enableWarnings
                && this == dispatchQueue.rootDispatch) {
                Log.w(TAG, "No DispatchQueueController set for dispatch queue with id: $queueId. " +
                        "Not setting a DispatchQueueController can cause memory leaks for long running tasks.")
            }
        }
    }

    override fun start(): Dispatch<R> {
        return start(null)
    }

    override fun start(errorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)?): Dispatch<R> {
        dispatchQueue.errorHandler = errorHandler
        if (!isCancelled) {
            dispatchQueue.completedDispatchQueue = false
            dispatchQueue.rootDispatch.runDispatcher()
        } else {
            if (enableWarnings) {
                Log.d(TAG, "Start called on dispatch queue with id: $queueId after it has already been cancelled.")
            }
        }
        return this
    }

    private fun handleException(throwable: Throwable) {
        val mainErrorHandler = dispatchQueue.errorHandler
        if (mainErrorHandler != null) {
            Threader.uiHandler.post {
                mainErrorHandler.invoke(throwable, this)
            }
            cancel()
            return
        }
        val globalHandler = globalErrorHandler
        if (globalHandler != null) {
            Threader.uiHandler.post {
                globalHandler.invoke(throwable, this)
            }
            cancel()
            return
        }
        cancel()
        throw throwable
    }

    override fun cancel(): Dispatch<R> {
        if (!isCancelled) {
            dispatchQueue.isCancelled = true
            val dispatchQueueController = dispatchQueue.dispatchQueueController
            dispatchQueue.dispatchQueueController = null
            if (dispatchQueueController is ActivityDispatchQueueController) {
                dispatchQueueController.unmanage(this)
            } else {
                dispatchQueueController?.unmanage(this)
            }
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
        }
        return this
    }

    private fun removeDispatcher() {
        handler.removeCallbacks(dispatcher)
        dispatchObservable.removeAllObservers()
        if (closeHandler) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                handler.looper.quitSafely()
            } else {
                handler.looper.quit()
            }
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
        return this
    }

    override fun managedBy(activity: Activity): Dispatch<R> {
        return managedBy(activity, CancelType.DESTROYED)
    }

    override fun managedBy(activity: Activity, cancelType: CancelType): Dispatch<R> {
        val oldDispatchQueueController = this.dispatchQueue.dispatchQueueController
        this.dispatchQueue.dispatchQueueController = null
        oldDispatchQueueController?.unmanage(this)
        val dispatchQueueController = ActivityDispatchQueueController.getInstance(activity)
        this.dispatchQueue.dispatchQueueController = dispatchQueueController
        dispatchQueueController.manage(this, cancelType)
        return this
    }

    override fun <U> post(func: (R) -> U): Dispatch<U> {
        return post(0, func)
    }

    override fun <U> post(delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        return getNewDispatch(func, Threader.uiHandler, delayInMillis, false)
    }

    override fun <U> async(func: (R) -> U): Dispatch<U> {
        return async(0, func)
    }

    override fun <U> async(backgroundHandler: Handler, func: (R) -> U): Dispatch<U> {
        return async(backgroundHandler, 0, func)
    }

    override fun <U> async(delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        val workHandler = when {
            handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> handler
        }
        return getNewDispatch(func, workHandler, delayInMillis, workHandler == handler && closeHandler)
    }

    override fun <U> async(backgroundHandler: Handler, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        return getNewDispatch(func, backgroundHandler, delayInMillis, backgroundHandler == handler && closeHandler)
    }

    override fun <U> async(threadType: ThreadType, func: (R) -> U): Dispatch<U> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        val handlerPair = Threader.getHandlerPairForThreadType(threadType)
        return getNewDispatch(func, handlerPair.first, 0, handlerPair.second)
    }

    override fun <U> async(threadType: ThreadType, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        val handlerPair = Threader.getHandlerPairForThreadType(threadType)
        return getNewDispatch(func, handlerPair.first, delayInMillis, handlerPair.second)
    }

    override fun <T> map(func: (R) -> T): Dispatch<T> {
        return async(func)
    }

    private fun <T, R> getNewDispatch(worker: (T) -> R,
                                      handler: Handler,
                                      delayInMillis: Long,
                                      closeHandler: Boolean): Dispatch<R> {
        val newDispatch = DispatchImpl(
            dispatchId = getNewDispatchId(),
            handler = handler,
            delayInMillis = delayInMillis,
            worker = worker,
            closeHandler = closeHandler,
            dispatchQueue = dispatchQueue,
            dispatchType = DISPATCH_TYPE_NORMAL)
        newDispatch.dispatchSources.add(this)
        if (!isCancelled) {
            dispatchQueue.queue.add(newDispatch)
            if (dispatchQueue.completedDispatchQueue) {
                dispatchQueue.completedDispatchQueue = false
                newDispatch.runDispatcher()
            }
        }
        return newDispatch
    }

    override fun <U> zipWith(dispatch: Dispatch<U>): Dispatch<Pair<R, U>> {
        val workHandler = when {
            handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> handler
        }
        val zipDispatch = dispatch as DispatchImpl<*, *>
        val zipRootDispatch = zipDispatch.rootDispatch as DispatchImpl<*, *>
        val zipDispatchQueue = zipRootDispatch.dispatchQueue
        val newDispatch = DispatchImpl<Pair<R, U>, Pair<R, U>>(
            dispatchId = getNewDispatchId(),
            handler = workHandler,
            delayInMillis = 0,
            worker = { it },
            closeHandler = (workHandler == handler) && closeHandler,
            dispatchQueue = dispatchQueue,
            dispatchType = DISPATCH_TYPE_NORMAL)
        newDispatch.dispatchSources.add(this)
        newDispatch.dispatchSources.add(dispatch)
        val zipWorkHandler = when {
            zipDispatch.handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> zipDispatch.handler
        }
        val zipQueueDispatch = DispatchImpl<Any?, Any?>(
            dispatchId = getNewDispatchId(),
            handler = zipWorkHandler,
            delayInMillis = 0,
            worker = {
                if (!isCancelled) {
                    dispatchQueue.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
                it
            },
            closeHandler = (zipWorkHandler == handler) && zipDispatch.closeHandler,
            dispatchQueue = zipDispatchQueue,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
        zipQueueDispatch.dispatchSources.add(dispatch)
        val queueDispatch = DispatchImpl<R, R>(
            dispatchId = getNewDispatchId(),
            handler = workHandler,
            delayInMillis = 0,
            worker = {
                if (!zipDispatchQueue.isCancelled) {
                    if (zipDispatchQueue.completedDispatchQueue) {
                        zipDispatchQueue.completedDispatchQueue = false
                        zipQueueDispatch.runDispatcher()
                    } else {
                        zipDispatchQueue.rootDispatch.runDispatcher()
                    }
                }
                it
            },
            closeHandler = (workHandler == handler) && closeHandler,
            dispatchQueue = dispatchQueue,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
        queueDispatch.dispatchSources.add(this)
        val dispatchQueueController = dispatchQueue.dispatchQueueController
        val zipDispatchQueueController = zipDispatchQueue.dispatchQueueController
        if (zipDispatchQueueController == null && dispatchQueueController != null) {
            zipRootDispatch.managedBy(dispatchQueueController)
        }
        val errorHandler = dispatchQueue.errorHandler
        val zipErrorHandler = zipDispatchQueue.errorHandler
        if (zipErrorHandler == null && errorHandler != null) {
            zipDispatchQueue.errorHandler = errorHandler
        }
        if (!zipDispatchQueue.isCancelled) {
            zipDispatchQueue.queue.add(zipQueueDispatch)
        }
        if (!isCancelled) {
            dispatchQueue.queue.add(queueDispatch)
            dispatchQueue.queue.add(newDispatch)
            if (dispatchQueue.completedDispatchQueue) {
                dispatchQueue.completedDispatchQueue = false
                queueDispatch.runDispatcher()
            }
        }
        return newDispatch
    }

    override fun <U, T> zipWith(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R, U, T>> {
        val zipDispatch = dispatch as DispatchImpl<*, *>
        val zipRootDispatch = zipDispatch.rootDispatch as DispatchImpl<*, *>
        val zipDispatchQueue = zipRootDispatch.dispatchQueue
        val zipDispatch2 = dispatch2 as DispatchImpl<*, *>
        val zipRootDispatch2 = zipDispatch2.rootDispatch as DispatchImpl<*, *>
        val zipDispatchQueue2 = zipRootDispatch2.dispatchQueue
        val workHandler = when {
            handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> handler
        }
        val newDispatch = DispatchImpl<Triple<R, U, T>, Triple<R, U, T>>(
            dispatchId = getNewDispatchId(),
            handler = workHandler,
            delayInMillis = 0,
            worker = { it },
            closeHandler = (workHandler == handler) && closeHandler,
            dispatchQueue = dispatchQueue,
            dispatchType = DISPATCH_TYPE_NORMAL)
        newDispatch.dispatchSources.add(this)
        newDispatch.dispatchSources.add(dispatch)
        newDispatch.dispatchSources.add(dispatch2)
        val zipWorkHandler2 = when {
            zipDispatch2.handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> zipDispatch2.handler
        }
        val zipQueueDispatch2 = DispatchImpl<Any?, Any?>(
            dispatchId = getNewDispatchId(),
            handler = zipWorkHandler2,
            delayInMillis = 0,
            worker = {
                if (!dispatchQueue.isCancelled) {
                    dispatchQueue.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
                it
            },
            closeHandler = (zipWorkHandler2 == handler) && zipDispatch2.closeHandler,
            dispatchQueue = zipDispatchQueue2,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
        zipQueueDispatch2.dispatchSources.add(dispatch2)
        val zipWorkHandler = when {
            zipDispatch.handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> zipDispatch.handler
        }
        val zipQueueDispatch = DispatchImpl<Any?, Any?>(
            dispatchId = getNewDispatchId(),
            handler = zipWorkHandler,
            delayInMillis = 0,
            worker = {
                if (!zipDispatchQueue2.isCancelled) {
                    if (zipDispatchQueue2.completedDispatchQueue) {
                        zipDispatchQueue2.completedDispatchQueue = false
                        zipQueueDispatch2.runDispatcher()
                    } else {
                        zipDispatchQueue2.rootDispatch.runDispatcher()
                    }
                }
                it
            },
            closeHandler = (zipWorkHandler == handler) && zipDispatch.closeHandler,
            dispatchQueue = zipDispatchQueue,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
        zipQueueDispatch.dispatchSources.add(dispatch)
        val queueDispatch = DispatchImpl<R, R>(
            dispatchId = getNewDispatchId(),
            handler = workHandler,
            delayInMillis = 0,
            worker = {
                if (!zipDispatchQueue.isCancelled) {
                    if (zipDispatchQueue.completedDispatchQueue) {
                        zipDispatchQueue.completedDispatchQueue = false
                        zipQueueDispatch.runDispatcher()
                    } else {
                        zipDispatchQueue.rootDispatch.runDispatcher()
                    }
                }
                it
            },
            closeHandler = (workHandler == handler) && closeHandler,
            dispatchQueue = dispatchQueue,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
        queueDispatch.dispatchSources.add(this)
        val dispatchQueueController = dispatchQueue.dispatchQueueController
        val zipDispatchQueueController = zipDispatchQueue.dispatchQueueController
        if (zipDispatchQueueController == null && dispatchQueueController != null) {
            zipRootDispatch.managedBy(dispatchQueueController)
        }
        val zipDispatchQueueController2 = zipDispatchQueue2.dispatchQueueController
        if (zipDispatchQueueController2 == null && dispatchQueueController != null) {
            zipRootDispatch2.managedBy(dispatchQueueController)
        }
        val errorHandler = dispatchQueue.errorHandler
        val zipErrorHandler = zipDispatchQueue.errorHandler
        if (zipErrorHandler == null && errorHandler != null) {
            zipDispatchQueue.errorHandler = errorHandler
        }
        val zipErrorHandler2 = zipDispatchQueue2.errorHandler
        if (zipErrorHandler2 == null && errorHandler != null) {
            zipDispatchQueue2.errorHandler = errorHandler
        }
        if (!zipDispatchQueue2.isCancelled) {
            zipDispatchQueue2.queue.add(zipQueueDispatch2)
        }
        if (!zipDispatchQueue.isCancelled) {
            zipDispatchQueue.queue.add(zipQueueDispatch)
        }
        if(!isCancelled) {
            dispatchQueue.queue.add(queueDispatch)
            dispatchQueue.queue.add(newDispatch)
            if (dispatchQueue.completedDispatchQueue) {
                dispatchQueue.completedDispatchQueue = false
                queueDispatch.runDispatcher()
            }
        }
        return newDispatch
    }

    override fun <U, T> zipWithAny(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R?, U?, T?>> {
        val zipDispatch = dispatch as DispatchImpl<*, *>
        val zipRootDispatch = zipDispatch.rootDispatch as DispatchImpl<*, *>
        val zipDispatchQueue = zipRootDispatch.dispatchQueue
        val zipDispatch2 = dispatch2 as DispatchImpl<*, *>
        val zipRootDispatch2 = zipDispatch2.rootDispatch as DispatchImpl<*, *>
        val zipDispatchQueue2 = zipRootDispatch2.dispatchQueue
        val workHandler = when {
            handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> handler
        }
        val newDispatch = DispatchImpl<Triple<R?, U?, T?>, Triple<R?, U?, T?>>(
            dispatchId = getNewDispatchId(),
            handler = workHandler,
            delayInMillis = 0,
            worker = { it },
            closeHandler = (workHandler == handler) && closeHandler,
            dispatchQueue = dispatchQueue,
            dispatchType = DISPATCH_TYPE_ANY_RESULT)
        newDispatch.dispatchSources.add(this)
        newDispatch.dispatchSources.add(dispatch)
        newDispatch.dispatchSources.add(dispatch2)
        val zipWorkHandler = when {
            zipDispatch.handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> zipDispatch.handler
        }
        val zipQueueDispatch = DispatchImpl<Any?, Any?>(
            dispatchId = getNewDispatchId(),
            handler = zipWorkHandler,
            delayInMillis = 0,
            worker = {
                if (!dispatchQueue.isCancelled) {
                    dispatchQueue.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
                it
            },
            closeHandler = (zipWorkHandler == handler) && zipDispatch.closeHandler,
            dispatchQueue = zipDispatchQueue,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
        zipQueueDispatch.dispatchSources.add(dispatch)
        val zipWorkHandler2 = when {
            zipDispatch2.handler.looper.thread.name == Threader.uiHandler.looper.thread.name -> Threader.backgroundHandler
            else -> zipDispatch2.handler
        }
        val zipQueueDispatch2 = DispatchImpl<Any?, Any?>(
            dispatchId = getNewDispatchId(),
            handler = zipWorkHandler2,
            delayInMillis = 0,
            worker = {
                if (!dispatchQueue.isCancelled) {
                    dispatchQueue.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
                it
            },
            closeHandler = (zipWorkHandler2 == handler) && zipDispatch2.closeHandler,
            dispatchQueue = zipDispatchQueue2,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER
        )
        zipQueueDispatch2.dispatchSources.add(dispatch2)
        val queueDispatch = DispatchImpl<R, R>(
            dispatchId = getNewDispatchId(),
            handler = workHandler,
            delayInMillis = 0,
            worker = {
                if (!zipDispatchQueue.isCancelled) {
                    if (zipDispatchQueue.completedDispatchQueue) {
                        zipDispatchQueue.completedDispatchQueue = false
                        zipQueueDispatch.runDispatcher()
                    } else {
                        zipDispatchQueue.rootDispatch.runDispatcher()
                    }
                }
                if (!zipDispatchQueue2.isCancelled) {
                    if (zipDispatchQueue2.completedDispatchQueue) {
                        zipDispatchQueue2.completedDispatchQueue = false
                        zipQueueDispatch2.runDispatcher()
                    } else {
                        zipDispatchQueue2.rootDispatch.runDispatcher()
                    }
                }
                it
            },
            closeHandler = (workHandler == handler) && closeHandler,
            dispatchQueue = dispatchQueue,
            dispatchType = DISPATCH_TYPE_QUEUE_RUNNER
        )
        queueDispatch.dispatchSources.add(this)
        val dispatchQueueController = dispatchQueue.dispatchQueueController
        val zipDispatchQueueController = zipDispatchQueue.dispatchQueueController
        if (zipDispatchQueueController == null && dispatchQueueController != null) {
            zipRootDispatch.managedBy(dispatchQueueController)
        }
        val zipDispatchQueueController2 = zipDispatchQueue2.dispatchQueueController
        if (zipDispatchQueueController2 == null && dispatchQueueController != null) {
            zipRootDispatch2.managedBy(dispatchQueueController)
        }
        val errorHandler = dispatchQueue.errorHandler
        val zipErrorHandler = zipDispatchQueue.errorHandler
        if (zipErrorHandler == null && errorHandler != null) {
            zipDispatchQueue.errorHandler = errorHandler
        }
        val zipErrorHandler2 = zipDispatchQueue2.errorHandler
        if (zipErrorHandler2 == null && errorHandler != null) {
            zipDispatchQueue2.errorHandler = errorHandler
        }
        if (!zipDispatchQueue2.isCancelled) {
            zipDispatchQueue2.queue.add(zipQueueDispatch2)
        }
        if (!zipDispatchQueue.isCancelled) {
            zipDispatchQueue.queue.add(zipQueueDispatch)
        }
        if (!isCancelled) {
            dispatchQueue.queue.add(queueDispatch)
            dispatchQueue.queue.add(newDispatch)
            if (dispatchQueue.completedDispatchQueue) {
                dispatchQueue.completedDispatchQueue = false
                queueDispatch.runDispatcher()
            }
        }
        return newDispatch
    }

    override fun cancelOnComplete(cancel: Boolean): Dispatch<R> {
        dispatchQueue.cancelOnComplete = cancel
        if (cancel && dispatchQueue.completedDispatchQueue) {
            cancel()
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

    override fun getDispatchObservable(): DispatchObservable<R> {
        return dispatchObservable
    }

    override fun setDispatchId(id: String): Dispatch<R> {
        this.dispatchId = id
        return this
    }

}