package com.tonyodev.dispatch

import android.os.Handler
import com.tonyodev.dispatch.internals.DispatchImpl
import com.tonyodev.dispatch.internals.DispatchQueue
import com.tonyodev.dispatch.utils.DISPATCH_TYPE_NORMAL
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.getNewDispatchId
import com.tonyodev.dispatch.utils.getNewQueueId
import com.tonyodev.dispatch.utils.throwIfUsesMainThreadForBackgroundWork
import java.lang.IllegalArgumentException

/**
 * Dispatcher is a simple and flexible work scheduler that schedulers work on a background or UI thread correctly
 * in the form of Dispatch objects in queues using the android.os.Handler class to handle threading.
 * @see Dispatcher.createDispatchQueue() to get started.
 */
object Dispatcher {

    /**
     * Enable or disable log warnings by the library.
     * @param enabled value. Disabled by default.
     * */
    @JvmStatic
    var enableLogWarnings = false

    /**
     * Sets the global error handler for Dispatch objects. This error handler is called only
     * if a dispatch queue does not handler its errors. The error handler is called on the main thread.
     * @param errorHandler the error handler. Notifies of the dispatch that throw the error and the error that was thrown.
     * */
    @JvmStatic
    var globalErrorHandler: ((throwable: Throwable, dispatch: Dispatch<*>) -> Unit)? = null

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch object returned will use the default background handler to schedule work in the background.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatchQueue(): Dispatch<Unit> {
        return createNewDispatch(
            handler = Threader.backgroundHandler,
            delayInMillis = 0,
            closeHandler = false,
            isIntervalDispatch = false,
            isTestDispatch = false)
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
        return createNewDispatch(
            handler = backgroundHandler ?: Threader.backgroundHandler,
            delayInMillis = 0,
            closeHandler = false,
            isIntervalDispatch = false,
            isTestDispatch = false)
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The returned dispatch queue will have a handler of the thread type
     * @param threadType the default threadType to use.
     * handler is used.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createDispatchQueue(threadType: ThreadType): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        val handlerPair = Threader.getHandlerPairForThreadType(threadType)
        return createNewDispatch(
            handler = handlerPair.first,
            delayInMillis = 0,
            closeHandler = handlerPair.second,
            isIntervalDispatch = false,
            isTestDispatch = false)
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
        return createNewDispatch(
            handler = Threader.getNewDispatchHandler(handlerName),
            delayInMillis = 0,
            closeHandler = true,
            isIntervalDispatch = false,
            isTestDispatch = false)
    }

    /**
     * Creates a new timer dispatch queue. A new handler thread is created to start the timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the handler runs the dispatch.
     * Values less than 1 indicates that there are no delays.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long): Dispatch<Unit> {
        return createNewDispatch(
            handler = Threader.getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = true,
            isIntervalDispatch = false,
            isTestDispatch = false)
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
        return createNewDispatch(
            handler = backgroundHandler ?: Threader.getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = backgroundHandler == null,
            isIntervalDispatch = false,
            isTestDispatch = false)
    }

    /**
     * Creates a new timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long, threadType: ThreadType): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        val handlerPair = Threader.getHandlerPairForThreadType(threadType)
        return createNewDispatch(
            handler = handlerPair.first,
            delayInMillis = delayInMillis,
            closeHandler = handlerPair.second,
            isIntervalDispatch = false,
            isTestDispatch = false)
    }

    /**
     * Creates a new interval dispatch queue that fires every x time. A new handler thread is created to start the interval dispatch.
     * @param delayInMillis the delay in milliseconds before the handler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long): Dispatch<Unit> {
        return createNewDispatch(
            handler = Threader.getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = true,
            isIntervalDispatch = true,
            isTestDispatch = false)
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
        return createNewDispatch(
            handler = backgroundHandler ?: Threader.getNewDispatchHandler(),
            delayInMillis = delayInMillis,
            closeHandler = backgroundHandler == null,
            isIntervalDispatch = true,
            isTestDispatch = false)
    }

    /**
     * Creates a new interval dispatch queue that fires every x time.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return new dispatch.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long, threadType: ThreadType): Dispatch<Unit> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        val handlerPair = Threader.getHandlerPairForThreadType(threadType)
        return createNewDispatch(
            handler = handlerPair.first,
            delayInMillis = delayInMillis,
            closeHandler = handlerPair.second,
            isIntervalDispatch = true,
            isTestDispatch = false)
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default background handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val backgroundDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = Threader.getHandlerPairForThreadType(ThreadType.BACKGROUND)
            return createNewDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false,
                isTestDispatch = false)
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the secondary background handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val backgroundSecondaryDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = Threader.getHandlerPairForThreadType(ThreadType.BACKGROUND_SECONDARY)
            return createNewDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false,
                isTestDispatch = false)
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default io handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val ioDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = Threader.getHandlerPairForThreadType(ThreadType.IO)
            return createNewDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false,
                isTestDispatch = false)
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default network handler/thread.
     * @return new dispatch.
     * */
    @JvmStatic
    val networkDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = Threader.getHandlerPairForThreadType(ThreadType.NETWORK)
            return createNewDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false,
                isTestDispatch = false)
        }

    /**
     * Creates a new test dispatch queue. All async and post run on the same thread this dispatch was created on.
     * Note: Test Dispatch queues do not run with delays.
     * @return test dispatch.
     * */
    @JvmStatic
    val testDispatchQueue: Dispatch<Unit>
        get() {
            val handlerPair = Threader.getHandlerPairForThreadType(ThreadType.MAIN)
            return createNewDispatch(
                handler = handlerPair.first,
                delayInMillis = 0,
                closeHandler = handlerPair.second,
                isIntervalDispatch = false,
                isTestDispatch = true)
        }
    
    private fun createNewDispatch(handler: Handler,
                                  delayInMillis: Long,
                                  closeHandler: Boolean,
                                  isIntervalDispatch: Boolean,
                                  isTestDispatch: Boolean): Dispatch<Unit> {
        val dispatchQueueData = DispatchQueue(
            queueId = getNewQueueId(),
            isIntervalDispatch = isIntervalDispatch,
            cancelOnComplete = !isIntervalDispatch,
            isTestDispatchQueue = isTestDispatch
        )
        val newDispatch = DispatchImpl<Unit, Unit>(
            dispatchId = getNewDispatchId(),
            handler = handler,
            delayInMillis = delayInMillis,
            worker = null,
            closeHandler = closeHandler,
            dispatchQueue = dispatchQueueData,
            dispatchType = DISPATCH_TYPE_NORMAL
        )
        dispatchQueueData.rootDispatch = newDispatch
        dispatchQueueData.queue.add(newDispatch)
        return newDispatch
    }

}