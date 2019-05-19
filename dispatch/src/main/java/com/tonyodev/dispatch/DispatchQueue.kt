package com.tonyodev.dispatch

import com.tonyodev.dispatch.internals.DispatchQueueImpl
import com.tonyodev.dispatch.internals.DispatchQueueInfo
import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import com.tonyodev.dispatch.thread.ThreadHandler
import com.tonyodev.dispatch.utils.*
import com.tonyodev.dispatch.utils.Threader
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
interface DispatchQueue<R> {

    /**
     * The dispatch queue id.
     * */
    val id: Int

    /***
     * The async or post label.
     * */
    val blockLabel: String

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
     * @param dispatchQueueErrorCallback the error handler for the dispatch queue. Notifies of the async or post block via its label that throw the error and the error that was thrown. Only called
     * if the dispatch queue object who throw the error does not handle it's error within its doOnError method. The error handler is called on the main thread.
     * @return dispatch queue.
     * */
    fun start(dispatchQueueErrorCallback: DispatchQueueErrorCallback?): DispatchQueue<R>

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
     * @param dispatchQueueController the dispatch controller that will manage the dispatch queue. Can be null.
     * @return dispatch queue.
     * */
    fun managedBy(dispatchQueueController: DispatchQueueController?): DispatchQueue<R>

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
     * Sets the dispatch queue object blockLabel. Use this blockLabel to identify where errors occur in the dispatch queue.
     * @param blockLabel the dispatch object block Label.
     * */
    fun setBlockLabel(blockLabel: String): DispatchQueue<R>

    /**
     * Transforms the data from the previous dispatch queue object to a another
     * data type. The transformation is performed on the background thread.
     * @param func the function.
     * @return the dispatch queue.
     * */
    fun <T> map(func: (R) -> T): DispatchQueue<T>

    /**
     * Gets the backing dispatch queue object DispatchQueueObservable.
     * @return DispatchQueueObservable
     * */
    fun getDispatchQueueObservable(): DispatchQueueObservable<R>

    /**
     * Adds a dispatch queue observer.
     * @param dispatchQueueObserver the observer.
     * @return the dispatch queue.
     * */
    fun addObserver(dispatchQueueObserver: DispatchQueueObserver<R>): DispatchQueue<R>

    /**
     * Adds a list of dispatch queue observers.
     * @param dispatchQueueObservers the collection of observers.
     * @return the dispatch queue.
     * */
    fun addObservers(dispatchQueueObservers: Collection<DispatchQueueObserver<R>>): DispatchQueue<R>

    /**
     * Removes a dispatch queue observer.
     * @param dispatchQueueObserver the observer to be removed.
     * @return the dispatch queue.
     * */
    fun removeObserver(dispatchQueueObserver: DispatchQueueObserver<R>): DispatchQueue<R>

    /**
     * Remove a list of dispatch queue observers.
     * @param dispatchQueueObservers the collection of observers to be removed.
     * @return the dispatch queue.
     * */
    fun removeObservers(dispatchQueueObservers: Collection<DispatchQueueObserver<R>>): DispatchQueue<R>

    /**
     * Removes all dispatch queue observers.
     * @return the dispatch queue.
     * */
    fun removeObservers(): DispatchQueue<R>

    /**
     * Retry the post or async operation if an error occurred.
     * @param count the retry count.
     * @param delayInMillis the delay in milliseconds before the next retry attempted.
     * @return the dispatch queue.
     * */
    fun retry(count: Int, delayInMillis: Long): DispatchQueue<R>

    /**
     * Retry the post or async operation if an error occurred.
     * @param count the retry count.
     * @return the dispatch queue.
     * */
    fun retry(count: Int): DispatchQueue<R>

    companion object Queue {

        /** DispatchQueue Global globalSettings.*/
        val globalSettings: Settings

        /**
         * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
         * @return new dispatch queue.
         * */
        fun createDispatchQueue(): DispatchQueue<Void?> {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.NEW)
            )
        }

        /**
         * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
         * @param backgroundHandler the background handler used to schedule work in the background.
         * @throws Exception throws exception if backgroundHandler thread passed in uses the ui thread.
         * @return new dispatch queue.
         * */
        fun createDispatchQueue(backgroundHandler: ThreadHandler): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
            startThreadHandlerIfNotActive(backgroundHandler)
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false)
            )
        }

        /**
         * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
         * The returned dispatch queue will have a handler of the thread type
         * @param threadType the default threadType to use.
         * handler is used.
         * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
         * @return new dispatch queue.
         * */
        fun createDispatchQueue(threadType: ThreadType): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(threadType)
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(threadType)
            )
        }

        /**
         * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
         * The returned dispatch will have a newly created handler that will handle background work.
         * @param threadHandlerName the name used by the threadHandler.
         * handler is used.
         * @return new dispatch queue.
         * */
        fun createDispatchQueue(threadHandlerName: String): DispatchQueue<Void?> {
            return createNewDispatchQueue(
                delayInMillis = 0,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(threadHandlerName)
            )
        }

        /**
         * Creates a new timer dispatch queue. A new handler thread is created to start the timer dispatch queue.
         * @param delayInMillis the delay in milliseconds before the handler runs the dispatch.
         * Values less than 1 indicates that there are no delays.
         * @return new dispatch queue.
         * */
        fun createTimerDispatchQueue(delayInMillis: Long): DispatchQueue<Void?> {
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.NEW)
            )
        }

        /**
         * Creates a new timer dispatch queue.
         * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
         * Values less than 1 indicates that there are no delays.
         * @param backgroundHandler the background handler used for the timer task.
         * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
         * @return new dispatch queue.
         * */
        fun createTimerDispatchQueue(delayInMillis: Long, backgroundHandler: ThreadHandler): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
            startThreadHandlerIfNotActive(backgroundHandler)
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false)
            )
        }

        /**
         * Creates a new timer dispatch queue.
         * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
         * Values less than 1 indicates that there are no delays.
         * @param threadType the thread type.
         * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
         * @return new dispatch queue.
         * */
        fun createTimerDispatchQueue(delayInMillis: Long, threadType: ThreadType): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(threadType)
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = false,
                threadHandlerInfo = Threader.getHandlerThreadInfo(threadType)
            )
        }

        /**
         * Creates a new interval dispatch queue that fires every x time. A new handler thread is created to start the interval dispatch.
         * @param delayInMillis the delay in milliseconds before the handler runs the worker.
         * Values less than 1 indicates that there are no delays.
         * @return new dispatch queue.
         * */
        fun createIntervalDispatchQueue(delayInMillis: Long): DispatchQueue<Void?> {
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = true,
                threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.NEW)
            )
        }

        /**
         * Creates a new interval dispatch queue that fires every x time.
         * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
         * Values less than 1 indicates that there are no delays.
         * @param backgroundHandler the background handler used for the timer task. If null, a new backgroundHandler is created.
         * @throws IllegalArgumentException if the backgroundHandler passed in uses the main thread to do background work.
         * @return new dispatch queue.
         * */
        fun createIntervalDispatchQueue(delayInMillis: Long, backgroundHandler: ThreadHandler): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(backgroundHandler)
            startThreadHandlerIfNotActive(backgroundHandler)
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = true,
                threadHandlerInfo = Threader.ThreadHandlerInfo(backgroundHandler, false)
            )
        }

        /**
         * Creates a new interval dispatch queue that fires every x time.
         * @param delayInMillis the delay in milliseconds before the backgroundHandler runs the worker.
         * Values less than 1 indicates that there are no delays.
         * @param threadType the thread type.
         * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
         * @return new dispatch queue.
         * */
        fun createIntervalDispatchQueue(delayInMillis: Long, threadType: ThreadType): DispatchQueue<Void?> {
            throwIfUsesMainThreadForBackgroundWork(threadType)
            return createNewDispatchQueue(
                delayInMillis = delayInMillis,
                isIntervalDispatchQueue = true,
                threadHandlerInfo = Threader.getHandlerThreadInfo(threadType)
            )
        }

        /**
         * Creates a new dispatch queue using the default background thread that can be used to post work on the main thread or do work in the background.
         * The dispatch queue operates on the default background handler/thread.
         * @return new dispatch queue.
         * */
        val background: DispatchQueue<Void?>
            get() {
                return createNewDispatchQueue(
                    delayInMillis = 0,
                    isIntervalDispatchQueue = false,
                    threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND)
                )
            }

        /**
         * Creates a new dispatch queue using the io thread that can be used to post work on the main thread or do work in the background.
         * The dispatch queue operates on the default io handler/thread.
         * @return new dispatch queue.
         * */
        val io: DispatchQueue<Void?>
            get() {
                return createNewDispatchQueue(
                    delayInMillis = 0,
                    isIntervalDispatchQueue = false,
                    threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.IO)
                )
            }

        /**
         * Creates a new test dispatch queue using the test thread. All async and post run on the same thread this dispatch queue was created on.
         * Note: Test Dispatch queues do not run with delays.
         * @return test dispatch queue.
         * */
        val test: DispatchQueue<Void?>
            get() {
                return createNewDispatchQueue(
                    delayInMillis = 0,
                    isIntervalDispatchQueue = false,
                    threadHandlerInfo = Threader.getHandlerThreadInfo(ThreadType.TEST)
                )
            }

        private fun createNewDispatchQueue(delayInMillis: Long, isIntervalDispatchQueue: Boolean, threadHandlerInfo: Threader.ThreadHandlerInfo): DispatchQueue<Void?> {
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

        init {
            val settings = Settings()
            globalSettings = settings
            forceLoadAndroidSettingsIfAvailable(settings)
        }

    }

}