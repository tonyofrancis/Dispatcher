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
 * in the form of Dispatch objects in queues using the android.os.Handler class to handle threading.
 * @see Dispatcher.createDispatchQueue() to get started.
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
    private var backgroundSecondaryHandler: Handler? = null
        get() {
            if (field == null) {
                val handlerThread = HandlerThread("dispatcherBackgroundSecondary")
                handlerThread.start()
                field = Handler(handlerThread.looper)
            }
            return field
        }
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
    @Volatile
    private var newThreadCount = 0
    private var enableWarnings = false
    private var globalErrorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null

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
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch object returned will use the default background handler to schedule work in the background.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatchQueue(): Dispatch<Unit> {
        return createFreshDispatch(
            handler = getBackgroundHandler(),
            delayInMillis = 0,
            closeHandler = false,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * @param backgroundHandler the background handler used to schedule work in the background. If null, the default background handler is used.
     * @throws Exception throws exception if backgroundHandler thread passed in uses the ui thread.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatchQueue(backgroundHandler: Handler?): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        return createFreshDispatch(
            handler = backgroundHandler ?: getBackgroundHandler(),
            delayInMillis = 0,
            closeHandler = false,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The returned dispatch queue will have a handler of the thread type
     * @param threadType the default threadType to use.
     * handler is used.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatchQueue(threadType: ThreadType): Dispatch<Unit> {
        val handlerPair = getHandlerPairForThreadType(threadType)
        return createFreshDispatch(
            handler = handlerPair.first,
            delayInMillis = 0,
            closeHandler = handlerPair.second,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The returned dispatch will have a newly created handler that will handle background work.
     * @param handlerName the name used by the handler.
     * handler is used.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatchQueue(handlerName: String): Dispatch<Unit> {
        return createFreshDispatch(
            handler = getNewDispatchHandler(handlerName),
            delayInMillis = 0,
            closeHandler = true,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new timer dispatch queue. A new handler thread is created to start the timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the handler runs the dispatch.
     * Values less than 1 indicates that there are no delays.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long): Dispatch<Unit> {
        return createFreshDispatch(
            handler = getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = true,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param backgroundHandler the background handler used for the timer task. If null, a new backgroundHandler is created.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long, backgroundHandler: Handler?): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        return createFreshDispatch(
            handler = backgroundHandler ?: getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = backgroundHandler == null,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long, threadType: ThreadType): Dispatch<Unit> {
        val handlerPair = getHandlerPairForThreadType(threadType)
        return createFreshDispatch(
            handler = handlerPair.first,
            delayInMillis = delayInMillis,
            closeHandler = handlerPair.second,
            isIntervalDispatch = false)
    }

    /**
     * Creates a new interval dispatch queue that fires every x time. A new handler thread is created to start the interval dispatch.
     * @param delayInMillis the delay in milliseconds before the handler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long): Dispatch<Unit> {
        return createFreshDispatch(
            handler = getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = true,
            isIntervalDispatch = true)
    }

    /**
     * Creates a new interval dispatch queue that fires every x time.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param backgroundHandler the background handler used for the timer task. If null, a new backgroundHandler is created.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long, backgroundHandler: Handler?): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        return createFreshDispatch(
            handler = backgroundHandler ?: getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = backgroundHandler == null,
            isIntervalDispatch = true)
    }

    /**
     * Creates a new interval dispatch queue that fires every x time.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long, threadType: ThreadType): Dispatch<Unit> {
        val handlerPair = getHandlerPairForThreadType(threadType)
        return createFreshDispatch(
            handler = handlerPair.first,
            delayInMillis = delayInMillis,
            closeHandler = handlerPair.second,
            isIntervalDispatch = true)
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default background handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val backgroundDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = getHandlerPairForThreadType(ThreadType.BACKGROUND)
            return createFreshDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false)
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the secondary background handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val backgroundSecondaryDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = getHandlerPairForThreadType(ThreadType.BACKGROUND_SECONDARY)
            return createFreshDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false)
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default io handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val ioDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = getHandlerPairForThreadType(ThreadType.IO)
            return createFreshDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false)
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default network handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val networkDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = getHandlerPairForThreadType(ThreadType.NETWORK)
            return createFreshDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false)
        }

    private fun getBackgroundHandler(): Handler {
        return  backgroundHandler
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
            ThreadType.BACKGROUND_SECONDARY -> Pair(backgroundSecondaryHandler!!, false)
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
        val dispatchQueueData = DispatchQueue(queueId = getNewQueueId(),
            isIntervalDispatch = isIntervalDispatch,
            cancelOnComplete = !isIntervalDispatch)
        val newDispatch = DispatchInfo<Unit, Unit>(
            dispatchId = getNewDispatchId(),
            handler = handler,
            delayInMillis = delayInMillis,
            worker = null,
            closeHandler = closeHandler,
            dispatchQueue = dispatchQueueData,
            dispatchType = DISPATCH_TYPE_NORMAL)
        dispatchQueueData.rootDispatch = newDispatch
        dispatchQueueData.queue.add(newDispatch)
        return newDispatch
    }

    private class DispatchQueue(val queueId: Int,
                                val isIntervalDispatch: Boolean = false,
                                var cancelOnComplete: Boolean) {
        @Volatile
        var isCancelled = false
        @Volatile
        var completedDispatchQueue = false
        lateinit var rootDispatch: DispatchInfo<*, *>
        val queue = LinkedList<DispatchInfo<*, *>>()
        var errorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null
        var dispatchQueueController: DispatchQueueController? = null
    }

    private class DispatchInfo<T, R>(override var dispatchId: String,
                                     private val handler: Handler,
                                     private val delayInMillis: Long = 0,
                                     private val worker: ((T) -> R)?,
                                     private val closeHandler: Boolean,
                                     private val dispatchQueue: DispatchQueue,
                                     private val dispatchType: Int): Dispatch<R> {

        private val dispatchSources = ArrayList<Dispatch<*>?>(3)
        private val dispatchObserversSet = mutableSetOf<DispatchObserver<R>>()

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
            if (!isCancelled) {
                val iterator = dispatchObserversSet.iterator()
                val result = results as R
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

        private fun getNextDispatchInfo(after: Dispatch<*>): DispatchInfo<*, *>? {
            val iterator = dispatchQueue.queue.iterator()
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
                dispatchQueue.isCancelled = true
                val dispatchQueueController = dispatchQueue.dispatchQueueController
                dispatchQueue.dispatchQueueController = null
                if (dispatchQueueController is ActivityDispatchQueueController) {
                    dispatchQueueController.unmanage(this)
                } else {
                    dispatchQueueController?.unmanage(this)
                }
                val iterator = dispatchQueue.queue.iterator()
                var dispatch: DispatchInfo<*, *>?
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
            val iterator = dispatchObserversSet.iterator()
            while (iterator.hasNext()) {
                iterator.next()
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
            return getNewDispatch(func, uiHandler, delayInMillis, false)
        }

        override fun <U> async(func: (R) -> U): Dispatch<U> {
            return async(0, func)
        }

        override fun <U> async(backgroundHandler: Handler, func: (R) -> U): Dispatch<U> {
            return async(backgroundHandler, 0, func)
        }

        override fun <U> async(delayInMillis: Long, func: (R) -> U): Dispatch<U> {
            val workHandler = when {
                handler.looper.thread.name == uiHandler.looper.thread.name -> getBackgroundHandler()
                else -> handler
            }
            return getNewDispatch(func, workHandler, delayInMillis, workHandler == handler && closeHandler)
        }

        override fun <U> async(backgroundHandler: Handler, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
            throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
            return getNewDispatch(func, backgroundHandler, delayInMillis, backgroundHandler == handler && closeHandler)
        }

        override fun <U> async(threadType: ThreadType, func: (R) -> U): Dispatch<U> {
            val handlerPair = getHandlerPairForThreadType(threadType)
            return getNewDispatch(func, handlerPair.first, 0, handlerPair.second)
        }

        override fun <U> async(threadType: ThreadType, delayInMillis: Long, func: (R) -> U): Dispatch<U> {
            val handlerPair = getHandlerPairForThreadType(threadType)
            return getNewDispatch(func, handlerPair.first, delayInMillis, handlerPair.second)
        }

        override fun <T> map(func: (R) -> T): Dispatch<T> {
            return async(func)
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
                handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> handler
            }
            val zipDispatch = dispatch as DispatchInfo<*, *>
            val zipRootDispatch = zipDispatch.rootDispatch as DispatchInfo<*, *>
            val zipDispatchQueue = zipRootDispatch.dispatchQueue
            val newDispatch = DispatchInfo<Pair<R, U>, Pair<R, U>>(
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
                zipDispatch.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch.handler
            }
            val zipQueueDispatch = DispatchInfo<Any?, Any?>(
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
            val queueDispatch = DispatchInfo<R, R>(
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
            val zipDispatch = dispatch as DispatchInfo<*, *>
            val zipRootDispatch = zipDispatch.rootDispatch as DispatchInfo<*, *>
            val zipDispatchQueue = zipRootDispatch.dispatchQueue
            val zipDispatch2 = dispatch2 as DispatchInfo<*, *>
            val zipRootDispatch2 = zipDispatch2.rootDispatch as DispatchInfo<*, *>
            val zipDispatchQueue2 = zipRootDispatch2.dispatchQueue
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
                dispatchQueue = dispatchQueue,
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
                zipDispatch.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch.handler
            }
            val zipQueueDispatch = DispatchInfo<Any?, Any?>(
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
            val queueDispatch = DispatchInfo<R, R>(
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
            val zipDispatch = dispatch as DispatchInfo<*, *>
            val zipRootDispatch = zipDispatch.rootDispatch as DispatchInfo<*, *>
            val zipDispatchQueue = zipRootDispatch.dispatchQueue
            val zipDispatch2 = dispatch2 as DispatchInfo<*, *>
            val zipRootDispatch2 = zipDispatch2.rootDispatch as DispatchInfo<*, *>
            val zipDispatchQueue2 = zipRootDispatch2.dispatchQueue
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
                dispatchQueue = dispatchQueue,
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
                zipDispatch2.handler.looper.thread.name == uiHandler.looper.thread.name -> backgroundHandler
                else -> zipDispatch2.handler
            }
            val zipQueueDispatch2 = DispatchInfo<Any?, Any?>(
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
            val queueDispatch = DispatchInfo<R, R>(
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
                    break
                }
            }
            return this
        }

        override fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R> {
            val iterator = dispatchObserversSet.iterator()
            var count = 0
            while (iterator.hasNext()) {
                if (dispatchObservers.contains(iterator.next())) {
                    iterator.remove()
                    ++count
                    if (count == dispatchObservers.size) {
                        break
                    }
                }
            }
            return this
        }

        override fun getDispatchObservable(): DispatchObservable<R, Dispatch<R>> {
            return this
        }

        override fun setDispatchId(id: String): Dispatch<R> {
            this.dispatchId = id
            return this
        }

    }

}