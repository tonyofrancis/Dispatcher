package com.tonyodev.dispatch

import android.app.Activity
import android.os.Handler
import java.lang.IllegalArgumentException

/**
 * A Dispatch is used to perform work and return data on the right thread at the right time.
 * Dispatch objects are chained to form a dispatch queue. Hence the reason a dispatch can pass data to another dispatch
 * further down in the queue. If the cancel method is called on a dispatch object, all other dispatch in its dispatch
 * queue will be cancelled.
 *
 * Example:
 * queueId 66 -> dispatch(async) -> dispatch(async) -> dispatch(post)
 * queueId 1 -> dispatch()
 * queueId 88 -> dispatch(post) -> dispatch(async)
 * queueId 78595 -> dispatch(post)
 * */
interface Dispatch<R> {

    /**
     * The dispatch queue id.
     * */
    val queueId: Int

    /***
     * The dispatch id.
     * */
    val dispatchId: String

    /**
     * The root dispatch in the queue.
     * */
    val rootDispatch: Dispatch<*>

    /**
     * Checks if a dispatch queue has been cancelled.
     * */
    val isCancelled: Boolean

    /**
     * Posts work on the UI thread.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> post(func: (R) -> U): Dispatch<U>

    /**
     * Posts work on the UI thread.
     * @param delayInMillis the delay in milliseconds before the dispatch runs the function.
     * Values less than 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> post(delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param func the func.
     * @return the dispatch.
     * */
    fun <U> async(func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param backgroundHandler the handler that will start the background work.
     * @param func the function.
     * @throws IllegalArgumentException if the handler passed in uses the main thread.
     * @return the dispatch.
     * */
    fun <U> async(backgroundHandler: Handler, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param threadType the threadType.
     * @param func the function.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return the dispatch.
     * */
    fun <U> async(threadType: ThreadType, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param delayInMillis the delay in milliseconds before the handler runs the func.
     * Values less than 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> async(delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param backgroundHandler the handler that will start the background work.
     * @param delayInMillis the delay in milliseconds before the handler runs the func.
     * Values less than 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> async(backgroundHandler: Handler, delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param threadType the threadType.
     * @param delayInMillis the delay in milliseconds before the handler runs the func.
     * Values less than indicates that there are no delays.
     * @param func the function.
     * @throws IllegalArgumentException if the passed in ThreadType is MAIN.
     * @return the dispatch.
     * */
    fun <U> async(threadType: ThreadType, delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Triggers the dispatch queue to start.
     * @param errorHandler the error handler for the dispatch queue. Notifies of the dispatch that throw the error and the error that was thrown. Only called
     * if a dispatch does not handle it's error within the doOnError method. The error handler is called on the main thread.
     * @return dispatch.
     * */
    fun start(errorHandler: ((Throwable, Dispatch<*>) -> Unit)?): Dispatch<R>

    /**
     * Triggers the dispatch queue to start.
     * @return dispatch.
     * */
    fun start(): Dispatch<R>

    /**
     * Cancels all pending work on the dispatch queue.
     * Once cancelled a dispatch queue cannot start again.
     * @return dispatch.
     * */
    fun cancel(): Dispatch<R>

    /**
     * If set, the error handler for this dispatch queue may not be called because the function will provide default data.
     * @param func the do on error function.
     * @return dispatch.
     * */
    fun doOnError(func: ((Throwable) -> R)): Dispatch<R>

    /**
     * Set's this dispatch queue to be managed by a DispatchQueueController.
     * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
     * @param dispatchQueueController the dispatch controller that will manage the dispatch queue.
     * @return dispatch.
     * */
    fun managedBy(dispatchQueueController: DispatchQueueController): Dispatch<R>

    /**
     * Set's this dispatch queue to be managed by an Activity. The activity is wrapped in an instance
     * of ActivityDispatchQueueController. ActivityDispatchQueueController is controlled by the activity's lifecycle.
     * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
     * @param activity the activity that will manage the dispatch queue. The cancel type is Destroyed. Cancellation
     * occurs when the activity's onDestroy method is called.
     * @return dispatch.
     * */
    fun managedBy(activity: Activity): Dispatch<R>

    /**
     * Set's this dispatch queue to be managed by an Activity. The activity is wrapped in an instance
     * of ActivityDispatchQueueController. ActivityDispatchQueueController is controlled by the activity's lifecycle.
     * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
     * @param activity the activity that will manage the dispatch queue.
     * @param cancelType the cancel type
     * @return dispatch.
     * */
    fun managedBy(activity: Activity, cancelType: CancelType): Dispatch<R>

    /**
     * Set's this dispatch queue to be managed by a LifecycleDispatchQueueController.
     * LifecycleDispatchQueueController is controlled by an Android component's lifecycle.
     * @param lifecycleDispatchQueueController the lifecycleDispatchQueueController that will manage the dispatch queue.
     * The cancel type is Destroyed. Cancellation occurs when the Android components's onDestroy method is called.
     * @return dispatch.
     * */
    fun managedBy(lifecycleDispatchQueueController: LifecycleDispatchQueueController): Dispatch<R>

    /**
     * Set's this dispatch queue to be managed by a LifecycleDispatchQueueController.
     * LifecycleDispatchQueueController is controlled by an Android component's lifecycle.
     * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
     * @param lifecycleDispatchQueueController the lifecycleDispatchQueueController that will manage the dispatch queue.
     * @param cancelType the cancel type
     * @return dispatch.
     * */
    fun managedBy(lifecycleDispatchQueueController: LifecycleDispatchQueueController, cancelType: CancelType): Dispatch<R>

    /**
     * Zips this dispatch with the passed in dispatch and returns a paired result. Both dispatch objects
     * are still controlled by their respective dispatch queues.
     * If the passed in dispatch is not managed by a DispatchQueueController and the current dispatch is managed,
     * the passed in dispatch will then be managed by this dispatch's DispatchQueueController.
     * @param dispatch dispatch to zipWith with.
     * @return dispatch with result pair.
     * */
    fun <U> zipWith(dispatch: Dispatch<U>): Dispatch<Pair<R, U>>

    /**
     * Zips this dispatch with the passed in dispatches and returns a triple result.
     * If the passed in dispatch objects are not managed by a DispatchQueueController and the current dispatch is managed,
     * the passed in dispatch objects will then be managed by this dispatch's DispatchQueueController.
     * @param dispatch dispatch to zipWith with.
     * @param dispatch2 dispatch to zipWith with.
     * @return dispatch with result triple.
     * */
    fun <U, T> zipWith(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R, U, T>>

    /**
     * Zips this dispatch with the passed in dispatches and returns a triple result. A triple
     * result is returned when all or any of the dispatches gets processed. All dispatch objects
     * are still controlled by their respective dispatch queues.
     * If the passed in dispatch objects are not managed by a DispatchQueueController and the current dispatch is managed,
     * the passed in dispatch objects will then be managed by this dispatch's DispatchQueueController.
     * @param dispatch dispatch to zipWith with.
     * @param dispatch2 dispatch to zipWith with.
     * @return dispatch with result triple.
     * */
    fun <U, T> zipWithAny(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R?, U?, T?>>

    /**
     * Cancels the dispatch queue when all dispatch objects have been processed/handled. If a Dispatch
     * Controller is managing the dispatch queue. This value has no effect. The DispatchQueueController will
     * handle the queue's cancellation.
     * @param cancel true or false. True by default for non interval dispatch queues.
     * @return dispatch
     * */
    fun cancelOnComplete(cancel: Boolean): Dispatch<R>

    /**
     * Sets the dispatch object id. Use this id to identify where errors occur in the dispatch queue.
     * @param id the dispatch object id.
     * */
    fun setDispatchId(id: String): Dispatch<R>

    /**
     * Transforms the data from the previous dispatch object to a another
     * data type.
     * @param func the function.
     * @return the new dispatch.
     * */
    fun <T> map(func: (R) -> T): Dispatch<T>

    /**
     * Gets the backing dispatch Observable.
     * @return DispatchObservable
     * */
    fun getDispatchObservable(): DispatchObservable<R>

    /**
     * Adds a dispatch observer.
     * @param dispatchObserver the observer.
     * @return the dispatch.
     * */
    fun addObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R>

    /**
     * Adds a list of dispatch observers.
     * @param dispatchObservers the list of observers.
     * @return the dispatch.
     * */
    fun addObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R>

    /**
     * Removes a dispatch observer.
     * @param dispatchObserver the observer to be removed.
     * @return the dispatch.
     * */
    fun removeObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R>

    /**
     * Remove a list of dispatch observers.
     * @param dispatchObservers the list of observers to be removed.
     * @return the dispatch.
     * */
    fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R>

}