package com.tonyodev.dispatch

import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController

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

}