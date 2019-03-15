package com.tonyodev.dispatch

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList

/**
 * Dispatcher is a simple and flexible work scheduler that schedulers work on a background or UI thread correctly
 * in the form of Dispatch objects using the android.os.Handler class to handle threading.
 * @see Dispatcher.createDispatch() to get started.
 */
object Dispatcher {

    private const val TAG = "com.tonyodev.dispatch"
    private const val DISPATCH_TYPE_NORMAL = 0
    private const val DISPATCH_TYPE_QUEUE_RUNNER = 1
    private const val DISPATCH_TYPE_ANY_RESULT = 2
    private val INVALID_RESULT = InvalidResult()

    private val uiHandler = Handler(Looper.getMainLooper())
    private val backgroundHandler: Handler = {
        val handlerThread = HandlerThread("dispatcherBackground")
        handlerThread.start()
        Handler(handlerThread.looper)
    }()
    private var networkHandler: Handler? = null
        get() {
            if (field == null) {
                val handlerThread = HandlerThread("dispatcherNetwork")
                handlerThread.start()
                field = Handler(handlerThread.looper)
            }
            return field
        }
    private var ioHandler: Handler? = null
        get() {
            if (field == null) {
                val handlerThread = HandlerThread("dispatcherIO")
                handlerThread.start()
                field = Handler(handlerThread.looper)
            }
            return field
        }
    private var globalErrorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null
    @Volatile
    private var newThreadCount = 0
    private var enableWarnings = false

    /**
     * Sets the global error handler for Dispatch objects. This error handler is called only
     * if a dispatch queue does not handler its errors. The error handler is called on the main thread.
     * @param errorHandler the error handler. Notifies of the dispatch that throw the error and the error that was thrown.
     * */
    @JvmStatic
    fun setGlobalErrorHandler(errorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)?) {
        this.globalErrorHandler = errorHandler
    }

    /**
     * Enable or disable log warnings by the library.
     * @param enabled value. Disabled by default.
     * */
    @JvmStatic
    fun setEnableLogWarnings(enabled: Boolean) {
        this.enableWarnings = enabled
    }

    /**
     * Creates a new dispatch object that can be used to post work on the main thread or do work in the background.
     * The dispatch object returned will use the default background handler to schedule work in the background.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatch(): Dispatch<Unit> {
        return createFreshDispatch(
            handler = backgroundHandler,
            delayInMillis = 0,
            closeHandler = false,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new dispatch object that can be used to post work on the main thread or do work in the background.
     * @param backgroundHandler the background handler used to schedule work in the background. If null, the default background handler is used.
     * @throws Exception throws exception if backgroundHandler thread passed in uses the ui thread.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatch(backgroundHandler: Handler?): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        return createFreshDispatch(
            handler = backgroundHandler ?: this.backgroundHandler,
            delayInMillis = 0,
            closeHandler = false,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new dispatch object that can be used to post work on the main thread or do work in the background.
     * The returned dispatch will have a handler of the thread type
     * @param threadType the default threadType to use.
     * handler is used.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatch(threadType: ThreadType): Dispatch<Unit> {
        val handlerPair = getHandlerPairForThreadType(threadType)
        return createFreshDispatch(
            handler = handlerPair.first,
            delayInMillis = 0,
            closeHandler = handlerPair.second,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new dispatch object that can be used to post work on the main thread or do work in the background.
     * The returned dispatch will have a newly created handler that will handle background work.
     * @param handlerName the name used by the handler.
     * handler is used.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatch(handlerName: String): Dispatch<Unit> {
        return createFreshDispatch(
            handler = getNewDispatchHandler(handlerName),
            delayInMillis = 0,
            closeHandler = true,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new timer dispatch. A new handler thread is created to run the timer dispatch.
     * @param delayInMillis the delay in milliseconds before the handler runs the dispatch.
     * Values under 1 indicates that there are no delays.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatch(delayInMillis: Long): Dispatch<Unit> {
        return createFreshDispatch(
            handler = getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = true,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new timer dispatch.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values under 1 indicates that there are no delays.
     * @param backgroundHandler the background handler used for the timer task. If null, a new backgroundHandler is created.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatch(delayInMillis: Long, backgroundHandler: Handler?): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        return createFreshDispatch(
            handler = backgroundHandler ?: getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = backgroundHandler == null,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new timer dispatch.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values under 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatch(delayInMillis: Long, threadType: ThreadType): Dispatch<Unit> {
        val handlerPair = getHandlerPairForThreadType(threadType)
        return createFreshDispatch(
            handler = handlerPair.first,
            delayInMillis = delayInMillis,
            closeHandler = handlerPair.second,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new interval dispatch that fires every x time. A new handler thread is created to run the interval dispatch.
     * @param delayInMillis the delay in milliseconds before the handler runs the worker.
     * Values under 1 indicates that there are no delays.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatch(delayInMillis: Long): Dispatch<Unit> {
        return createFreshDispatch(
            handler = getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = true,
            isIntervalDispatch = true)
    }

    /**
     * Creates a new interval dispatch that fires every x time.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values under 1 indicates that there are no delays.
     * @param backgroundHandler the background handler used for the timer task. If null, a new backgroundHandler is created.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatch(delayInMillis: Long, backgroundHandler: Handler?): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        return createFreshDispatch(
            handler = backgroundHandler ?: getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = backgroundHandler == null,
            isIntervalDispatch = true)
    }

    /**
     * Creates a new interval dispatch that fires every x time.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values under 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatch(delayInMillis: Long, threadType: ThreadType): Dispatch<Unit> {
        val handlerPair = getHandlerPairForThreadType(threadType)
        return createFreshDispatch(
            handler = handlerPair.first,
            delayInMillis = delayInMillis,
            closeHandler = handlerPair.second,
            isIntervalDispatch = true)
    }

    private fun getNewDispatchHandler(name: String? = null): Handler {
        val threadName = if (name == null || name.isEmpty()) {
            "dispatch${++newThreadCount}"
        } else {
            name
        }
        val handlerThread = HandlerThread(threadName)
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

    private fun getHandlerPairForThreadType(threadType: ThreadType): Pair<Handler, Boolean> {
        return when(threadType) {
            ThreadType.BACKGROUND -> Pair(backgroundHandler, false)
            ThreadType.IO -> Pair(ioHandler!!, false)
            ThreadType.NETWORK -> Pair(networkHandler!!, false)
            ThreadType.NEW -> Pair(getNewDispatchHandler(), true)
        }
    }

    private fun getNewQueueId(): Int {
        return UUID.randomUUID().hashCode()
    }

    private fun getNewDispatchId(): String {
        return UUID.randomUUID().toString()
    }

    private fun throwIfUsesMainThreadForBackgroundWork(handler: Handler?) {
        if (handler != null && handler.looper.thread.name == Looper.getMainLooper().thread.name) {
            throw IllegalArgumentException("Dispatch handler cannot use the main thread to perform background work." +
                    "Pass in a handler that uses a different thread.")
        }
    }

    private fun createFreshDispatch(handler: Handler,
                                    delayInMillis: Long,
                                    closeHandler: Boolean,
                                    isIntervalDispatch: Boolean): Dispatch<Unit> {
        val dispatchData = DispatchData(queueId = getNewQueueId(),
            isIntervalDispatch = isIntervalDispatch,
            cancelOnComplete = !isIntervalDispatch)
        val newDispatch = DispatchInfo<Unit, Unit>(
            dispatchId = getNewDispatchId(),
            handler = handler,
            delayInMillis = delayInMillis,
            worker = null,
            closeHandler = closeHandler,
            dispatchData = dispatchData,
            dispatchType = DISPATCH_TYPE_NORMAL)
        dispatchData.rootDispatch = newDispatch
        dispatchData.dispatchQueue.add(newDispatch)
        return newDispatch
    }

    private class DispatchData(val queueId: Int,
                               val isIntervalDispatch: Boolean = false,
                               var cancelOnComplete: Boolean) {
        @Volatile
        var isCancelled = false
        @Volatile
        var completedDispatchQueue = false
        lateinit var rootDispatch: DispatchInfo<*, *>
        val dispatchQueue = LinkedList<DispatchInfo<*, *>>()
        var errorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null
        var dispatchController: DispatchController? = null
    }

    private class DispatchInfo<T, R>(override val dispatchId: String,
                                     private val handler: Handler,
                                     private val delayInMillis: Long = -1,
                                     private val worker: ((T) -> R)?,
                                     private val closeHandler: Boolean,
                                     private val dispatchData: DispatchData,
                                     private val dispatchType: Int): Dispatch<R> {

        private val dispatchSources = ArrayList<Dispatch<*>?>(3)
        private val dispatchObserversSet = mutableSetOf<DispatchObserver<R>>()

        private var doOnErrorWorker: ((throwable: Throwable) -> R)? = null

        override val queueId: Int
            get() {
                return dispatchData.queueId
            }

        override val isCancelled: Boolean
            get() {
                return dispatchData.isCancelled
            }

        override val rootDispatch: Dispatch<*>
            get() {
                return dispatchData.rootDispatch
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
                                    if ((dispatchSources[1] as DispatchInfo<*, *>).results == INVALID_RESULT
                                        && (dispatchSources[2] as DispatchInfo<*, *>).results == INVALID_RESULT) {
                                        return@Runnable
                                    }
                                }
                                result1 = getSourceResult((dispatchSources[0] as DispatchInfo<*, *>))
                                result2 = getSourceResult((dispatchSources[1] as DispatchInfo<*, *>))
                                result3 = getSourceResult((dispatchSources[2] as DispatchInfo<*, *>))
                                if (hasInvalidResult(result1, result2, result3)) {
                                    INVALID_RESULT
                                } else {
                                    Triple(result1, result2, result3)
                                }
                            }
                            2 -> {
                                result1 = getSourceResult((dispatchSources[0] as DispatchInfo<*, *>))
                                result2 = getSourceResult((dispatchSources[1] as DispatchInfo<*, *>))
                                if (hasInvalidResult(result1, result2)) {
                                    INVALID_RESULT
                                } else {
                                    Pair(result1, result2)
                                }
                            }
                            else -> {
                                result1 = getSourceResult((dispatchSources[0] as DispatchInfo<*, *>))
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
            val iterator = dispatchObserversSet.iterator()
            val result = results as R
            uiHandler.post {
                while (iterator.hasNext()) {
                    iterator.next().onChanged(result)
                }
            }
        }

        private fun getSourceResult(sourceDispatchInfo: DispatchInfo<*, *>): Any? {
            var result = sourceDispatchInfo.results
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
                    dispatchData.isIntervalDispatch -> dispatchData.rootDispatch.runDispatcher()
                    else -> {
                        dispatchData.completedDispatchQueue = true
                        if (!dispatchData.isIntervalDispatch && dispatchData.cancelOnComplete) {
                            cancel()
                        }
                    }
                }
            }
        }

        private fun getNextDispatchInfo(after: Dispatch<*>): DispatchInfo<*, *>? {
            val iterator = dispatchData.dispatchQueue.iterator()
            var self: DispatchInfo<*, *>? = null
            var nextDispatch: DispatchInfo<*, *>? = null
            var dispatch: DispatchInfo<*, *>
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
                if (dispatchData.dispatchController == null && enableWarnings
                    && this == dispatchData.rootDispatch) {
                    Log.w(
                        TAG, "No DispatchController set for dispatch queue with id: $queueId. " +
                                "Not setting a DispatchController can cause memory leaks."
                    )
                }
            }
        }

        override fun run(): Dispatch<R> {
            return run(null)
        }

        override fun run(errorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)?): Dispatch<R> {
            dispatchData.errorHandler = errorHandler
            if (!isCancelled) {
                dispatchData.completedDispatchQueue = false
                dispatchData.rootDispatch.runDispatcher()
            }
            return this
        }

        private fun handleException(throwable: Throwable) {
            val mainErrorHandler = dispatchData.errorHandler
            if (mainErrorHandler != null) {
                uiHandler.post {
                    mainErrorHandler.invoke(throwable, this)
                }
                cancel()
                return
            }
            val globalHandler = globalErrorHandler
            if (globalHandler != null) {
                uiHandler.post {
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
                dispatchData.isCancelled = true
                val dispatchController = dispatchData.dispatchController
                dispatchData.dispatchController = null
                if (dispatchController is ActivityDispatchController) {
                    dispatchController.unmanage(this)
                } else {
                    dispatchController?.unmanage(this)
                }
                val iterator = dispatchData.dispatchQueue.iterator()
                var dispatch: DispatchInfo<*, *>?
                while (iterator.hasNext()) {
                    dispatch = iterator.next()
                    dispatch.removeDispatcher()
                    dispatch.dispatchSources.clear()
                    iterator.remove()
                }
            }
            return this
        }

        private fun removeDispatcher() {
            handler.removeCallbacks(dispatcher)
            val iterator = dispatchObserversSet.iterator()
            while (iterator.hasNext()) {
                iterator.remove()
            }
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

        override fun managedBy(dispatchController: DispatchController): Dispatch<R> {
            val oldDispatchController = this.dispatchData.dispatchController
            this.dispatchData.dispatchController = null
            oldDispatchController?.unmanage(this)
            this.dispatchData.dispatchController = dispatchController
            dispatchController.manage(this)
            return this
        }

        override fun managedBy(activity: Activity): Dispatch<R> {
            return managedBy(activity, CancelType.DESTROYED)
        }

        override fun managedBy(activity: Activity, cancelType: CancelType): Dispatch<R> {
            val oldDispatchController = this.dispatchData.dispatchController
            this.dispatchData.dispatchController = null
            oldDispatchController?.unmanage(this)
            val dispatchController = ActivityDispatchController.getInstance(activity)
            this.dispatchData.dispatchController = dispatchController
            dispatchController.manage(this, cancelType)
            return this
        }

        override fun <U> postMain(func: (R) -> U): Dispatch<U> {
            return postMain(0, func)
        }

        override fun <U> postMain(delayInMillis: Long, func: (R) -> U): Dispatch<U> {
            return getNewDispatch(func, uiHandler, delayInMillis, false)
        }

        override fun <U> doWork(func: (R) -> U): Dispatch<U> {
            return doWork(0, func)
        }

        override fun <U> doWork(backgroundHandler: Handler, func: (R) -> U): Dispatch<U> {
            return doWork(backgroundHandler, 0, func)
        }

        override fun <U> doWork(delayInMillis: Long, func: (R) -> U): Dispatch<U> {
            val workHandler = when {
                handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> handler
            }
            return getNewDispatch(func, workHandler, delayInMillis, workHandler == handler && closeHandler)
        }

        override fun <U> doWork(backgroundHandler: Handler, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
            throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
            return getNewDispatch(func, backgroundHandler, delayInMillis, backgroundHandler == handler && closeHandler)
        }

        override fun <U> doWork(threadType: ThreadType, func: (R) -> U): Dispatch<U> {
            val handlerPair = getHandlerPairForThreadType(threadType)
            return getNewDispatch(func, handlerPair.first, 0, handlerPair.second)
        }

        override fun <U> doWork(threadType: ThreadType, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
            val handlerPair = getHandlerPairForThreadType(threadType)
            return getNewDispatch(func, handlerPair.first, delayInMillis, handlerPair.second)
        }

        private fun <T, R> getNewDispatch(worker: (T) -> R,
                                          handler: Handler,
                                          delayInMillis: Long,
                                          closeHandler: Boolean): Dispatch<R> {
            val newDispatch = DispatchInfo(
                dispatchId = getNewDispatchId(),
                handler = handler,
                delayInMillis = delayInMillis,
                worker = worker,
                closeHandler = closeHandler,
                dispatchData = dispatchData,
                dispatchType = DISPATCH_TYPE_NORMAL)
            newDispatch.dispatchSources.add(this)
            if (!isCancelled) {
                dispatchData.dispatchQueue.add(newDispatch)
                if (dispatchData.completedDispatchQueue) {
                    dispatchData.completedDispatchQueue = false
                    newDispatch.runDispatcher()
                }
            }
            return newDispatch
        }

        override fun <U> zipWith(dispatch: Dispatch<U>): Dispatch<Pair<R, U>> {
            val workHandler = when {
                handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> handler
            }
            val zipDispatch = dispatch as DispatchInfo<*, *>
            val zipRootDispatch = zipDispatch.rootDispatch as DispatchInfo<*, *>
            val zipDispatchData = zipRootDispatch.dispatchData
            val newDispatch = DispatchInfo<Pair<R, U>, Pair<R, U>>(
                dispatchId = getNewDispatchId(),
                handler = workHandler,
                delayInMillis = 0,
                worker = { it },
                closeHandler = (workHandler == handler) && closeHandler,
                dispatchData = dispatchData,
                dispatchType = DISPATCH_TYPE_NORMAL)
            newDispatch.dispatchSources.add(this)
            newDispatch.dispatchSources.add(dispatch)
            val zipWorkHandler = when {
                zipDispatch.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch.handler
            }
            val zipQueueDispatch = DispatchInfo<Any?, Any?>(
                dispatchId = getNewDispatchId(),
                handler = zipWorkHandler,
                delayInMillis = 0,
                worker = {
                    if (!isCancelled) {
                        dispatchData.completedDispatchQueue = false
                        newDispatch.runDispatcher()
                    }
                    it
                },
                closeHandler = (zipWorkHandler == handler) && zipDispatch.closeHandler,
                dispatchData = zipDispatchData,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
            zipQueueDispatch.dispatchSources.add(dispatch)
            val queueDispatch = DispatchInfo<R, R>(
                dispatchId = getNewDispatchId(),
                handler = workHandler,
                delayInMillis = 0,
                worker = {
                    if (!zipDispatchData.isCancelled) {
                        if (zipDispatchData.completedDispatchQueue) {
                            zipDispatchData.completedDispatchQueue = false
                            zipQueueDispatch.runDispatcher()
                        } else {
                            zipDispatchData.rootDispatch.runDispatcher()
                        }
                    }
                    it
                },
                closeHandler = (workHandler == handler) && closeHandler,
                dispatchData = dispatchData,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
            queueDispatch.dispatchSources.add(this)
            val dispatchController = dispatchData.dispatchController
            val zipDispatchController = zipDispatchData.dispatchController
            if (zipDispatchController == null && dispatchController != null) {
                zipRootDispatch.managedBy(dispatchController)
            }
            zipDispatchData.dispatchQueue.add(zipQueueDispatch)
            dispatchData.dispatchQueue.add(queueDispatch)
            dispatchData.dispatchQueue.add(newDispatch)
            if (dispatchData.completedDispatchQueue) {
                dispatchData.completedDispatchQueue = false
                queueDispatch.runDispatcher()
            }
            return newDispatch
        }

        override fun <U, T> zipWith(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R, U, T>> {
            val zipDispatch = dispatch as DispatchInfo<*, *>
            val zipRootDispatch = zipDispatch.rootDispatch as DispatchInfo<*, *>
            val zipDispatchData = zipRootDispatch.dispatchData
            val zipDispatch2 = dispatch2 as DispatchInfo<*, *>
            val zipRootDispatch2 = zipDispatch2.rootDispatch as DispatchInfo<*, *>
            val zipDispatchData2 = zipRootDispatch2.dispatchData
            val workHandler = when {
                handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> handler
            }
            val newDispatch = DispatchInfo<Triple<R, U, T>, Triple<R, U, T>>(
                dispatchId = getNewDispatchId(),
                handler = workHandler,
                delayInMillis = 0,
                worker = { it },
                closeHandler = (workHandler == handler) && closeHandler,
                dispatchData = dispatchData,
                dispatchType = DISPATCH_TYPE_NORMAL)
            newDispatch.dispatchSources.add(this)
            newDispatch.dispatchSources.add(dispatch)
            newDispatch.dispatchSources.add(dispatch2)
            val zipWorkHandler2 = when {
                zipDispatch2.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch2.handler
            }
            val zipQueueDispatch2 = DispatchInfo<Any?, Any?>(
                dispatchId = getNewDispatchId(),
                handler = zipWorkHandler2,
                delayInMillis = 0,
                worker = {
                    if (!dispatchData.isCancelled) {
                        dispatchData.completedDispatchQueue = false
                        newDispatch.runDispatcher()
                    }
                    it
                },
                closeHandler = (zipWorkHandler2 == handler) && zipDispatch2.closeHandler,
                dispatchData = zipDispatchData2,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
            zipQueueDispatch2.dispatchSources.add(dispatch2)
            val zipWorkHandler = when {
                zipDispatch.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch.handler
            }
            val zipQueueDispatch = DispatchInfo<Any?, Any?>(
                dispatchId = getNewDispatchId(),
                handler = zipWorkHandler,
                delayInMillis = 0,
                worker = {
                    if (!zipDispatchData2.isCancelled) {
                        if (zipDispatchData2.completedDispatchQueue) {
                            zipDispatchData2.completedDispatchQueue = false
                            zipQueueDispatch2.runDispatcher()
                        } else {
                            zipDispatchData2.rootDispatch.runDispatcher()
                        }
                    }
                    it
                },
                closeHandler = (zipWorkHandler == handler) && zipDispatch.closeHandler,
                dispatchData = zipDispatchData,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
            zipQueueDispatch.dispatchSources.add(dispatch)
            val queueDispatch = DispatchInfo<R, R>(
                dispatchId = getNewDispatchId(),
                handler = workHandler,
                delayInMillis = 0,
                worker = {
                    if (!zipDispatchData.isCancelled) {
                      if (zipDispatchData.completedDispatchQueue) {
                          zipDispatchData.completedDispatchQueue = false
                          zipQueueDispatch.runDispatcher()
                      } else {
                          zipDispatchData.rootDispatch.runDispatcher()
                      }
                    }
                    it
                },
                closeHandler = (workHandler == handler) && closeHandler,
                dispatchData = dispatchData,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
            queueDispatch.dispatchSources.add(this)
            val dispatchController = dispatchData.dispatchController
            val zipDispatchController = zipDispatchData.dispatchController
            if (zipDispatchController == null && dispatchController != null) {
                zipRootDispatch.managedBy(dispatchController)
            }
            val zipDispatchController2 = zipDispatchData2.dispatchController
            if (zipDispatchController2 == null && dispatchController != null) {
                zipRootDispatch2.managedBy(dispatchController)
            }
            zipDispatchData2.dispatchQueue.add(zipQueueDispatch2)
            zipDispatchData.dispatchQueue.add(zipQueueDispatch)
            dispatchData.dispatchQueue.add(queueDispatch)
            dispatchData.dispatchQueue.add(newDispatch)
            if (dispatchData.completedDispatchQueue) {
                dispatchData.completedDispatchQueue = false
                queueDispatch.runDispatcher()
            }
            return newDispatch
        }

        override fun <U, T> zipWithAny(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R?, U?, T?>> {
            val zipDispatch = dispatch as DispatchInfo<*, *>
            val zipRootDispatch = zipDispatch.rootDispatch as DispatchInfo<*, *>
            val zipDispatchData = zipRootDispatch.dispatchData
            val zipDispatch2 = dispatch2 as DispatchInfo<*, *>
            val zipRootDispatch2 = zipDispatch2.rootDispatch as DispatchInfo<*, *>
            val zipDispatchData2 = zipRootDispatch2.dispatchData
            val workHandler = when {
                handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> handler
            }
            val newDispatch = DispatchInfo<Triple<R?, U?, T?>, Triple<R?, U?, T?>>(
                dispatchId = getNewDispatchId(),
                handler = workHandler,
                delayInMillis = 0,
                worker = { it },
                closeHandler = (workHandler == handler) && closeHandler,
                dispatchData = dispatchData,
                dispatchType = DISPATCH_TYPE_ANY_RESULT)
            newDispatch.dispatchSources.add(this)
            newDispatch.dispatchSources.add(dispatch)
            newDispatch.dispatchSources.add(dispatch2)
            val zipWorkHandler = when {
                zipDispatch.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch.handler
            }
            val zipQueueDispatch = DispatchInfo<Any?, Any?>(
                dispatchId = getNewDispatchId(),
                handler = zipWorkHandler,
                delayInMillis = 0,
                worker = {
                    if (!dispatchData.isCancelled) {
                        dispatchData.completedDispatchQueue = false
                        newDispatch.runDispatcher()
                    }
                    it
                },
                closeHandler = (zipWorkHandler == handler) && zipDispatch.closeHandler,
                dispatchData = zipDispatchData,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER)
            zipQueueDispatch.dispatchSources.add(dispatch)
            val zipWorkHandler2 = when {
                zipDispatch2.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch2.handler
            }
            val zipQueueDispatch2 = DispatchInfo<Any?, Any?>(
                dispatchId = getNewDispatchId(),
                handler = zipWorkHandler2,
                delayInMillis = 0,
                worker = {
                    if (!dispatchData.isCancelled) {
                        dispatchData.completedDispatchQueue = false
                        newDispatch.runDispatcher()
                    }
                    it
                },
                closeHandler = (zipWorkHandler2 == handler) && zipDispatch2.closeHandler,
                dispatchData = zipDispatchData2,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER
            )
            zipQueueDispatch2.dispatchSources.add(dispatch2)
            val queueDispatch = DispatchInfo<R, R>(
                dispatchId = getNewDispatchId(),
                handler = workHandler,
                delayInMillis = 0,
                worker = {
                    if (!zipDispatchData.isCancelled) {
                        if (zipDispatchData.completedDispatchQueue) {
                            zipDispatchData.completedDispatchQueue = false
                            zipQueueDispatch.runDispatcher()
                        } else {
                            zipDispatchData.rootDispatch.runDispatcher()
                        }
                    }
                    if (!zipDispatchData2.isCancelled) {
                        if (zipDispatchData2.completedDispatchQueue) {
                            zipDispatchData2.completedDispatchQueue = false
                            zipQueueDispatch2.runDispatcher()
                        } else {
                            zipDispatchData2.rootDispatch.runDispatcher()
                        }
                    }
                    it
                },
                closeHandler = (workHandler == handler) && closeHandler,
                dispatchData = dispatchData,
                dispatchType = DISPATCH_TYPE_QUEUE_RUNNER
            )
            queueDispatch.dispatchSources.add(this)
            val dispatchController = dispatchData.dispatchController
            val zipDispatchController = zipDispatchData.dispatchController
            if (zipDispatchController == null && dispatchController != null) {
                zipRootDispatch.managedBy(dispatchController)
            }
            val zipDispatchController2 = zipDispatchData2.dispatchController
            if (zipDispatchController2 == null && dispatchController != null) {
                zipRootDispatch2.managedBy(dispatchController)
            }
            zipDispatchData2.dispatchQueue.add(zipQueueDispatch2)
            zipDispatchData.dispatchQueue.add(zipQueueDispatch)
            dispatchData.dispatchQueue.add(queueDispatch)
            dispatchData.dispatchQueue.add(newDispatch)
            if (dispatchData.completedDispatchQueue) {
                dispatchData.completedDispatchQueue = false
                queueDispatch.runDispatcher()
            }
            return newDispatch
        }

        override fun cancelOnComplete(cancel: Boolean): Dispatch<R> {
            dispatchData.cancelOnComplete = cancel
            if (cancel && dispatchData.completedDispatchQueue) {
                cancel()
            }
            return this
        }

        override fun addObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R> {
            dispatchObserversSet.add(dispatchObserver)
            return this
        }

        override fun addObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R> {
            dispatchObserversSet.addAll(dispatchObservers)
            return this
        }

        override fun removeObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R> {
            val iterator = dispatchObserversSet.iterator()
            while (iterator.hasNext()) {
                if (dispatchObserver == iterator.next()) {
                    iterator.remove()
                }
            }
            return this
        }

        override fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R> {
            val iterator = dispatchObserversSet.iterator()
            while (iterator.hasNext()) {
                if (dispatchObservers.contains(iterator.next())) {
                    iterator.remove()
                }
            }
            return this
        }

        override fun getDispatchObservable(): DispatchObservable<R> {
            return this
        }

    }

}