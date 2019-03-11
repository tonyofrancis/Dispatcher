package com.tonyodev.dispatch

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
    fun <U> doWork(backgroundHandler: Handler?, delayInMillis: Long, func: (R) -> U): Dispatch<U>

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
     * Clones the passed in dispatch and combines it to this dispatch.
     * @param dispatch to be cloned.
     * @return dispatch with results from the current dispatch and the combines dispatch.
     * */
    fun <U> combine(dispatch: Dispatch<U>): Dispatch<Pair<R, U>>

    /**
     * Clones the passed in dispatch objects and combines it to this dispatch.
     * @param dispatch to be cloned.
     * @param dispatch2 to be cloned.
     * @return dispatch with results from the current dispatch and the combined dispach objects.
     * */
    fun <U, T> combine(dispatch: Dispatch<U>, dispatch2: Dispatch<T>): Dispatch<Triple<R, U, T>>

}