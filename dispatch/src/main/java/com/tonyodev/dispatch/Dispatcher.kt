package com.tonyodev.dispatch

import com.tonyodev.dispatch.internals.DispatchQueueImpl
import com.tonyodev.dispatch.internals.DispatchQueueInfo
import com.tonyodev.dispatch.thread.DefaultThreadHandlerFactory
import com.tonyodev.dispatch.thread.ThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandlerFactory
import com.tonyodev.dispatch.utils.*
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.getNewDispatchId
import com.tonyodev.dispatch.utils.getNewQueueId
import com.tonyodev.dispatch.utils.throwIfUsesMainThreadForBackgroundWork
import java.lang.IllegalArgumentException

/**
 * Dispatcher is a simple and flexible work scheduler that schedulers work on a background or UI thread correctly
 * in the form of DispatchQueues.
 * @see Dispatcher.createDispatchQueue() to get started.
 */
object Dispatcher {

    /**
     * Enable or disable log warnings by the library.
     * */
    @JvmStatic
    var enableLogWarnings = false

    /**
     * Sets the global error handler for Dispatch objects. This error handler is called only
     * if a dispatch queue does not handler its errors. The error handler is called on the main thread.
     * */
    @JvmStatic
    var globalErrorHandler: ((DispatchQueueError) -> Unit)? = null

    /**
     * Sets the global thread handler factory that is responsible for creating thread handlers that the dispatch queues
     * will use to process work in the background.
     * */
    @JvmStatic
    var threadHandlerFactory: ThreadHandlerFactory = DefaultThreadHandlerFactory()

    /**
     * Sets the logger uses by the library to report warnings and messages.
     * */
    @JvmStatic
    var logger: Logger = DefaultLogger()


    init {
        forceLoadAndroidClassesIfAvailable()
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch object returned will use the default background handler to schedule work in the background.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createDispatchQueue(): DispatchQueue<Void?> {
        return createNewDispatchQueue(
            delayInMillis = 0,
            isIntervalDispatchQueue = false,
            threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND))
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * @param backgroundHandler the background handler used to schedule work in the background.
     * @throws Exception throws exception if backgroundHandler thread passed in uses the ui thread.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createDispatchQueue(backgroundHandler: ThreadHandler): DispatchQueue<Void?> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        startThreadHandlerIfNotActive(backgroundHandler)
        return createNewDispatchQueue(
            delayInMillis = 0,
            isIntervalDispatchQueue = false,
            threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false))
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The returned dispatch queue will have a handler of the thread type
     * @param threadType the default threadType to use.
     * handler is used.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createDispatchQueue(threadType: ThreadType): DispatchQueue<Void?> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        return createNewDispatchQueue(
            delayInMillis = 0,
            isIntervalDispatchQueue = false,
            threadHandlerInfo = Threader.getHandlerThreadInfo(threadType))
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The returned dispatch will have a newly created handler that will handle background work.
     * @param handlerName the name used by the handler.
     * handler is used.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createDispatchQueue(handlerName: String): DispatchQueue<Void?> {
        return createNewDispatchQueue(
            delayInMillis = 0,
            isIntervalDispatchQueue = false,
            threadHandlerInfo = Threader.getHandlerThreadInfo(handlerName))
    }

    /**
     * Creates a new timer dispatch queue. A new handler thread is created to start the timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the handler runs the dispatch.
     * Values less than 1 indicates that there are no delays.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long): DispatchQueue<Void?> {
        return createNewDispatchQueue(
            delayInMillis = delayInMillis,
            isIntervalDispatchQueue = false,
            threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.NEW))
    }

    /**
     * Creates a new timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param backgroundHandler the background handler used for the timer task.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long, backgroundHandler: ThreadHandler): DispatchQueue<Void?> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        startThreadHandlerIfNotActive(backgroundHandler)
        return createNewDispatchQueue(
            delayInMillis = delayInMillis,
            isIntervalDispatchQueue = false,
            threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false))
    }

    /**
     * Creates a new timer dispatch queue.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createTimerDispatchQueue(delayInMillis: Long, threadType: ThreadType): DispatchQueue<Void?> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        return createNewDispatchQueue(
            delayInMillis = delayInMillis,
            isIntervalDispatchQueue = false,
            threadHandlerInfo = Threader.getHandlerThreadInfo(threadType))
    }

    /**
     * Creates a new interval dispatch queue that fires every x time. A new handler thread is created to start the interval dispatch.
     * @param delayInMillis the delay in milliseconds before the handler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long): DispatchQueue<Void?> {
        return createNewDispatchQueue(
            delayInMillis = delayInMillis,
            isIntervalDispatchQueue = true,
            threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.NEW))
    }

    /**
     * Creates a new interval dispatch queue that fires every x time.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param backgroundHandler the background handler used for the timer task. If null, a new backgroundHandler is created.
     * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long, backgroundHandler: ThreadHandler): DispatchQueue<Void?> {
        throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
        startThreadHandlerIfNotActive(backgroundHandler)
        return createNewDispatchQueue(
            delayInMillis = delayInMillis,
            isIntervalDispatchQueue = true,
            threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false))
    }

    /**
     * Creates a new interval dispatch queue that fires every x time.
     * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
     * Values less than 1 indicates that there are no delays.
     * @param threadType the thread type.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return new dispatch queue.
     * */
    @JvmStatic
    fun createIntervalDispatchQueue(delayInMillis: Long, threadType: ThreadType): DispatchQueue<Void?> {
        throwIfUsesMainThreadForBackgroundWork(threadType)
        return createNewDispatchQueue(
            delayInMillis = delayInMillis,
            isIntervalDispatchQueue = true,
            threadHandlerInfo = Threader.getHandlerThreadInfo(threadType))
    }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default background handler/thread.
     * @return new dispatch queue.
     * */
    @JvmStatic
    val background: DispatchQueue<Void?>
        get() {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND))
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the secondary background handler/thread.
     * @return new dispatch queue.
     * */
    @JvmStatic
    val backgroundSecondary: DispatchQueue<Void?>
        get() {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND_SECONDARY))
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default io handler/thread.
     * @return new dispatch queue.
     * */
    @JvmStatic
    val io: DispatchQueue<Void?>
        get() {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.IO))
        }

    /**
     * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
     * The dispatch queue operates on the default network handler/thread.
     * @return new dispatch queue.
     * */
    @JvmStatic
    val network: DispatchQueue<Void?>
        get() {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.NETWORK))
        }

    /**
     * Creates a new test dispatch queue. All async and post run on the same thread this dispatch queue was created on.
     * Note: Test Dispatch queues do not run with delays.
     * @return test dispatch queue.
     * */
    @JvmStatic
    val test: DispatchQueue<Void?>
        get() {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.TEST))
        }

    /**
     * Creates a new main dispatch queue. All async and post run on the main thread.
     * @return main dispatch queue.
     * */
    @JvmStatic
    val main: DispatchQueue<Void?>
        get() {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.MAIN))
        }

    private fun createNewDispatchQueue(delayInMillis: Long,
                                       isIntervalDispatchQueue: Boolean,
                                       threadHandlerInfo: Threader.ThreadHandlerInfo): DispatchQueue<Void?> {
        val dispatchQueueInfo = DispatchQueueInfo(
            queueId = getNewQueueId(),
            isIntervalDispatch = isIntervalDispatchQueue,
            threadHandlerInfo = threadHandlerInfo)
        val newDispatchQueue = DispatchQueueImpl<Void?, Void?>(
            blockLabel = getNewDispatchId(),
            delayInMillis = delayInMillis,
            worker = null,
            dispatchQueueInfo = dispatchQueueInfo,
            threadHandlerInfo = threadHandlerInfo)
        dispatchQueueInfo.rootDispatchQueue = newDispatchQueue
        dispatchQueueInfo.queue.add(newDispatchQueue)
        return newDispatchQueue
    }

}