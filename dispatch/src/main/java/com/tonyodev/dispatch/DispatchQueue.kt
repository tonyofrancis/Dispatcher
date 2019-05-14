package com.tonyodev.dispatch

import com.tonyodev.dispatch.internals.DispatchQueueInfo
import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import com.tonyodev.dispatch.thread.DefaultThreadHandlerFactory
import com.tonyodev.dispatch.thread.ThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandlerFactory
import com.tonyodev.dispatch.utils.*
import com.tonyodev.dispatch.utils.Threader
import com.tonyodev.dispatch.utils.forceLoadAndroidClassesIfAvailable
import com.tonyodev.dispatch.utils.getNewDispatchId
import com.tonyodev.dispatch.utils.getNewQueueId
import com.tonyodev.dispatch.utils.startThreadHandlerIfNotActive
import com.tonyodev.dispatch.utils.throwIfUsesMainThreadForBackgroundWork
import java.lang.IllegalArgumentException

/**
 * A DispatchQueue is used to perform work and return data on the right thread at the right time.
 * DispatchQueue objects are chained to form a queue. Hence the reason a dispatch queue object can pass data to another dispatch queue object
 * further down in the queue. If the cancel method is called on a dispatch queue object, all other dispatch queue objects in the queue will be cancelled.
 *
 * Example:
 * id 66 -> dispatchQueue(async) -> dispatchQueue(async) -> dispatchQueue(post)
 * id 1 -> dispatchQueue()
 * id 88 -> dispatchQueue(post) -> dispatchQueue(async)
 * id 78595 -> dispatchQueue(post)
 * */
class DispatchQueue<R> {

    /**
     * The dispatch queue id.
     * */
    val id: Int

    /***
     * The dispatch queue object id.
     * */
    val dispatchId: String

    /**
     * The first dispatch queue object in the queue.
     * */
    val root: DispatchQueue<*>

    /**
     * Checks if a dispatch queue has been cancelled.
     * */
    val isCancelled: Boolean

    /**
     * Gets the DispatchQueueController that is managing this dispatch queue. May be null.
     * */
    val controller: DispatchQueueController?

    /**
     * Posts work on the main thread.
     * @param func the function.
     * @return the dispatch queue.
     * */
    fun <U> post(func: (R) -> U): DispatchQueue<U>

    /**
     * Posts work on the main thread.
     * @param delayInMillis the delay in milliseconds before the dispatch runs the function.
     * Values less than 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch queue.
     * */
    fun <U> post(delayInMillis: Long, func: (R) -> U): DispatchQueue<U>

    /**
     * Perform work on the background thread.
     * @param func the func.
     * @return the dispatch queue.
     * */
    fun <U> async(func: (R) -> U): DispatchQueue<U>

    /**
     * Perform work on the background thread.
     * @param delayInMillis the delay in milliseconds before the background thread runs the function.
     * Values less than 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch queue.
     * */
    fun <U> async(delayInMillis: Long, func: (R) -> U): DispatchQueue<U>

    /**
     * Triggers the dispatch queue to start.
     * @param errorHandler the error handler for the dispatch queue. Notifies of the dispatch that throw the error and the error that was thrown. Only called
     * if the dispatch queue object who throw the error does not handle it's error within its doOnError method. The error handler is called on the main thread.
     * @return dispatch queue.
     * */
    fun start(errorHandler: ((Throwable, String) -> Unit)?): DispatchQueue<R>

    /**
     * Triggers the dispatch queue to start.
     * @return dispatch queue.
     * */
    fun start(): DispatchQueue<R>

    /**
     * Cancels all pending work on the dispatch queue.
     * Once cancelled a dispatch queue cannot start again.
     * @return dispatch queue.
     * */
    fun cancel(): DispatchQueue<R>

    /**
     * The doOnError handles the case where an error occurred during a async or post operation. Each dispatch queue object
     * can have its own doOnError callback.
     * If set, the error handler for this dispatch queue may not be called because the function will provide default data.
     * This is called on the same thread the the error occurred.
     * @param func the do on error function. This function has to return data of the same type the preceding async or post dispatch
     * would have returned. The data returned here would most likely be some kind of default/generic data indicating an error occurred.
     * @return dispatch.
     * */
    fun doOnError(func: ((Throwable) -> R)): DispatchQueue<R>

    /**
     * Set's this dispatch queue to be managed by a DispatchQueueController.
     * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
     * @param dispatchQueueController the dispatch controller that will manage the dispatch queue.
     * @return dispatch queue.
     * */
    fun managedBy(dispatchQueueController: DispatchQueueController): DispatchQueue<R>

    /**
     * Set's this dispatch queue to be managed by a LifecycleDispatchQueueController.
     * LifecycleDispatchQueueController is controlled by a component's lifecycle.
     * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
     * @param lifecycleDispatchQueueController the lifecycleDispatchQueueController that will manage the dispatch queue.
     * @param cancelType the cancel type
     * @return dispatch queue.
     * */
    fun managedBy(lifecycleDispatchQueueController: LifecycleDispatchQueueController, cancelType: CancelType): DispatchQueue<R>

    /**
     * Combines this dispatchQueue with the passed in dispatchQueue by cloning the passed in dispatchQueue into this dispatchQueue's queue. Returns a paired result.
     * @param dispatchQueue dispatchQueue queue to combine with.
     * @return dispatchQueue queue with result pair.
     * */
    fun<U> zip(dispatchQueue: DispatchQueue<U>): DispatchQueue<Pair<R, U>>

    /**
     * Combines this dispatchQueue queue with the passed in dispatches by cloning the passed in dispatches into this dispatchQueue's queue.
     * Returns a Triple result.
     * @param dispatchQueue dispatchQueue to combine with.
     * @param dispatchQueue2 dispatchQueue to combine with.
     * @return dispatchQueue with result triple.
     * */
    fun<U, T> zip(dispatchQueue: DispatchQueue<U>, dispatchQueue2: DispatchQueue<T>): DispatchQueue<Triple<R, U, T>>

    /**
     * Sets the dispatch queue object dispatchId. Use this dispatchId to identify where errors occur in the dispatch queue.
     * @param dispatchId the dispatch object dispatchId.
     * */
    fun setDispatchId(dispatchId: String): DispatchQueue<R>

    /**
     * Transforms the data from the previous dispatch queue object to a another
     * data type. The transformation is performed on the background thread.
     * @param func the function.
     * @return the dispatch queue.
     * */
    fun <T> map(func: (R) -> T): DispatchQueue<T>

    /**
     * Gets the backing dispatch queue object DispatchObservable.
     * @return DispatchObservable
     * */
    fun getDispatchObservable(): DispatchObservable<R>

    /**
     * Adds a dispatch observer.
     * @param dispatchObserver the observer.
     * @return the dispatch queue.
     * */
    fun addObserver(dispatchObserver: DispatchObserver<R>): DispatchQueue<R>

    /**
     * Adds a list of dispatch observers.
     * @param dispatchObservers the list of observers.
     * @return the dispatch queue.
     * */
    fun addObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchQueue<R>

    /**
     * Removes a dispatch observer.
     * @param dispatchObserver the observer to be removed.
     * @return the dispatch queue.
     * */
    fun removeObserver(dispatchObserver: DispatchObserver<R>): DispatchQueue<R>

    /**
     * Remove a list of dispatch observers.
     * @param dispatchObservers the list of observers to be removed.
     * @return the dispatch queue.
     * */
    fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchQueue<R>

    /**
     * Removes all dispatch observers.
     * @return the dispatch queue.
     * */
    fun removeObservers(): DispatchQueue<R>

    companion object {

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
        var globalErrorHandler: ((throwable: Throwable, dispatch: DispatchQueue<*>, String) -> Unit)? = null

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
         * The dispatch queue object returned will use the default background handler to schedule work in the background.
         * @return new dispatch queue.
         * */
        @JvmStatic
        fun createDispatchQueue(): DispatchQueue<Void?> {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND))
        }

//        /**
//         * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
//         * The dispatch queue object returned will use the default background handler to schedule work in the background.
//         * @return new dispatch queue.
//         * */
//        @JvmStatic
//        fun<T> async(func: (Void?) -> T): DispatchQueue<T> {
//            return createNewDispatchQueue(
//                delayInMillis = 0,
//                isIntervalDispatchQueue = false,
//                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND))
//                .async(func)
//        }

        /**
         * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
         * @param backgroundThreadHandler the background thread handler used to schedule work in the background.
         * @throws Exception throws exception if threadHandler thread passed in uses the ui thread.
         * @return new dispatch queue.
         * */
        @JvmStatic
        fun createDispatchQueue(backgroundThreadHandler: ThreadHandler): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(backgroundThreadHandler)
            startThreadHandlerIfNotActive(backgroundThreadHandler)
            val threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundThreadHandler, false)
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = threadHandlerInfo)
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
         * The returned dispatch queue will have a newly created thread handler that will handle background work.
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
            val threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false)
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = threadHandlerInfo)
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
         * @param backgroundHandler the background handler used for the timer task.
         * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
         * @return new dispatch queue.
         * */
        @JvmStatic
        fun createIntervalDispatchQueue(delayInMillis: Long, backgroundHandler: ThreadHandler): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
            startThreadHandlerIfNotActive(backgroundHandler)
            val threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false)
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = true,
                threadHandlerInfo = threadHandlerInfo)
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
        val backgroundDispatchQueue: DispatchQueue<Void?>
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
        val backgroundSecondaryDispatchQueue: DispatchQueue<Void?>
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
        val ioDispatchQueue: DispatchQueue<Void?>
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
        val networkDispatchQueue: DispatchQueue<Void?>
            get() {
                return createNewDispatchQueue(
                    delayInMillis = 0,
                    isIntervalDispatchQueue = false,
                    threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.NETWORK))
            }

        /**
         * Creates a new test dispatch queue. All async and post run on the same thread this dispatch was created on.
         * Note: Test Dispatch queues do not run with delays.
         * @return test dispatch queue.
         * */
        @JvmStatic
        val testDispatchQueue: DispatchQueue<Void?>
            get() {
                return createNewDispatchQueue(
                    delayInMillis = 0,
                    isIntervalDispatchQueue = false,
                    threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.TEST))
            }

        private fun createNewDispatchQueue(delayInMillis: Long,
                                           isIntervalDispatchQueue: Boolean,
                                           threadHandlerInfo: Threader.ThreadHandlerInfo): DispatchQueue<Void?> {
            val dispatchQueueInfo = DispatchQueueInfo(
                queueId = getNewQueueId(),
                isIntervalDispatch = isIntervalDispatchQueue,
                threadHandlerInfo = threadHandlerInfo)
            val dispatchQueue = DispatchQueue<Void?>(
                dispatchId = getNewDispatchId(),
                delayInMillis = delayInMillis,
                worker = null,
                dispatchQueueInfo = dispatchQueueInfo,
                threadHandlerInfo = threadHandlerInfo)
            dispatchQueueInfo.rootDispatchQueue = dispatchQueue
            dispatchQueueInfo.queue.add(dispatchQueue)
            return dispatchQueue
        }

    }


}