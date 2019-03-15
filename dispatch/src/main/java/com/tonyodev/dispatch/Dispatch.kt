package com.tonyodev.dispatch

import android.app.Activity
import android.os.Handler
import java.lang.IllegalArgumentException

/**
 * A Dispatch is used to perform work on the right thread at the right time and only completes one unit of work.
 * Once it completes, it checks if there are any other dispatch objects in the queue with the same queue id and performs work
 * on that dispatch. Hence they are chained and can return the data from the previous dispatch.
 * If cancelled is called on a dispatch, all queued dispatch objects with the same queue id will cancelled.
 *
 * dispatch example:
 * id 66 -> dispatch(doWork) -> dispatch(doWork) -> dispatch(postMain)
 * id 1 -> dispatch()
 * id 88 -> dispatch(post) -> dispatch(doWork)
 * id 78595 -> dispatch(postMain)
 * */
interface Dispatch<R> {

    /**
     * the dispatch queue id.
     * */
    val queueId: Int

    /***
     * The block id.
     * */
    val dispatchId: String

    /**
     * The root dispatch in the chain.
     * */
    val rootDispatch: Dispatch<*>

    /**
     * Checks if a dispatch was cancelled.
     * */
    val isCancelled: Boolean

    /**
     * Posts work on the UI thread.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> postMain(func: (R) -> U): Dispatch<U>

    /**
     * Posts work on the UI thread.
     * @param delayInMillis the delay in milliseconds before the dispatch runs the function.
     * Values under 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> postMain(delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param func the func.
     * @return the dispatch.
     * */
    fun <U> doWork(func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param backgroundHandler the handler that will run the background work.
     * @param func the function.
     * @throws IllegalArgumentException if the handler passed in uses the main thread.
     * @return the dispatch.
     * */
    fun <U> doWork(backgroundHandler: Handler, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param threadType the threadType.
     * @param func the function.
     * @throws IllegalArgumentException if the handler passed in uses the main thread.
     * @return the dispatch.
     * */
    fun <U> doWork(threadType: ThreadType, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param delayInMillis the delay in milliseconds before the handler runs the func.
     * Values under 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> doWork(delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param backgroundHandler the handler that will run the background work.
     * @param delayInMillis the delay in milliseconds before the handler runs the func.
     * Values under 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> doWork(backgroundHandler: Handler, delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Perform work on the background thread.
     * @param threadType the threadType.
     * @param delayInMillis the delay in milliseconds before the handler runs the func.
     * Values under 1 indicates that there are no delays.
     * @param func the function.
     * @return the dispatch.
     * */
    fun <U> doWork(threadType: ThreadType, delayInMillis: Long, func: (R) -> U): Dispatch<U>

    /**
     * Triggers the dispatch to perform work. The dispatch do not perform work until run is called.
     * @param errorHandler the error handler. Notifies of the dispatch that throw the error and the error that was thrown. Can be null. Only called
     * if a block does not handle it's error within doOnErrorMethod.
     * @return dispatch.
     * */
    fun run(errorHandler: ((Throwable, Dispatch<*>) -> Unit)?): Dispatch<R>

    /**
     * Tiggers the dispatch to perform work. The dispatch do not perform work until run is called.
     * Run can only be called once.
     * @return dispatch.
     * */
    fun run(): Dispatch<R>

    /**
     * Cancels all pending work on the dispatch. All queued dispatch with the same queue id
     * will be cancelled. Once cancelled a dispatch queue cannot run again. It would be unsafe to do so.
     * @return dispatch.
     * */
    fun cancel(): Dispatch<R>

    /**
     * If set, the error handler for this dispatch is not called because the function will provide default data.
     * @param func the do on error function.
     * @return dispatch.
     * */
    fun doOnError(func: ((Throwable) -> R)): Dispatch<R>

    /**
     * Set's this dispatch to be managed by a DispatchController.
     * Managed dispatch objects can be cancelled by the DispatchController if the dispatch is not already cancelled.
     * @param dispatchController the dispatch controller that will manage the dispatch.
     * @return dispatch.
     * */
    fun managedBy(dispatchController: DispatchController): Dispatch<R>

    /**
     * Set's this dispatch to be managed by an Activity. The activity is wrapped in an instance
     * of ActivityDispatchController. This dispatch controller is controlled by the activity's lifecycle.
     * Managed dispatch objects can be cancelled by the DispatchController if the dispatch is not already cancelled.
     * @param activity the activity that will manage the dispatch. The cancel type is Destroyed.
     * @return dispatch.
     * */
    fun managedBy(activity: Activity): Dispatch<R>

    /**
     * Set's this dispatch to be managed by an Activity. The activity is wrapped in an instance
     * of ActivityDispatchController. This dispatch controller is controlled by the activity's lifecycle.
     * Managed dispatch objects can be cancelled by the DispatchController if the dispatch is not already cancelled.
     * @param activity the activity that will manage the dispatch.
     * @param cancelType the cancel type
     * @return dispatch.
     * */
    fun managedBy(activity: Activity, cancelType: CancelType): Dispatch<R>

    /**
     * Zips this dispatch with the passed in dispatch and returns a paired result.
     * If the passed in dispatch is not manager by a DispatchController and the current dispatch is managed.
     * The passed in dispatch will then be managed by this dispatch's DispatchController.
     * @param dispatch dispatch to zipWith with.
     * @return dispatch with result pair.
     * */
    fun <U> zipWith(dispatch: Dispatch<U>): Dispatch<Pair<R, U>>

    /**
     * Zips this dispatch with the passed in dispatches and returns a triple result.
     * If the passed in dispatch is not manager by a DispatchController and the current dispatch is managed.
     * The passed in dispatch will then be managed by this dispatch's DispatchController.
     * @param dispatch dispatch to zipWith with.
     * @param dispatch2 dispatch to zipWith with.
     * @return dispatch with result triple.
     * */
    fun <U, T> zipWith(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R, U, T>>

    /**
     * Zips this dispatch with the passed in dispatches and returns a triple result. A triple
     * result is returned when all or any of the dispatches gets processed.
     * If the passed in dispatch is not manager by a DispatchController and the current dispatch is managed.
     * The passed in dispatch will then be managed by this dispatch's DispatchController.
     * @param dispatch dispatch to zipWith with.
     * @param dispatch2 dispatch to zipWith with.
     * @return dispatch with result triple.
     * */
    fun <U, T> zipWithAny(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R?, U?, T?>>

}