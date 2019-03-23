package com.tonyodev.dispatch

import android.os.Handler

/**
 * Interface that allows for the attaching of dispatch observers.
 * */
interface DispatchObservable<R> {

    /**
     * Adds a dispatch observer.
     * @param dispatchObserver the observer.
     * @return the dispatchObservable.
     * */
    fun addObserver(dispatchObserver: DispatchObserver<R>): DispatchObservable<R>

    /**
     * Adds a list of dispatch observers.
     * @param dispatchObservers the list of observers.
     * @return the dispatchObservable.
     * */
    fun addObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchObservable<R>

    /**
     * Removes a dispatch observer.
     * @param dispatchObserver the observer to be removed.
     * @return the dispatchObservable.
     * */
    fun removeObserver(dispatchObserver: DispatchObserver<R>): DispatchObservable<R>

    /**
     * Remove a list of dispatch observers.
     * @param dispatchObservers the list of observers to be removed.
     * @return the dispatchObservable.
     * */
    fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): DispatchObservable<R>

    /** Notifies observers of the passed in result
     * @param result the result
     * @return the dispatchObservable.
     * */
    fun notify(result: R): DispatchObservable<R>


    companion object Factory {

        /**
         * Creates a new DispatchObservable that notifies observers of
         * the result on the ui thread.
         * @return dispatch observable
         * */
        fun <R> create(): DispatchObservable<R> {
            return create(ThreadType.MAIN)
        }

        /**
         * Creates a new DispatchObservable that notifies observers of
         * the result on the passed in handler.
         * @param handler the handler.
         * @return dispatch observable
         * */
        fun <R> create(handler: Handler): DispatchObservable<R> {
            return DispatchObservableImpl(handler, true)
        }

        /**
         * Creates a new DispatchObservable that notifies observers of
         * the result on the passed in thread type.
         * @param threadType the thread type
         * @return dispatch observable
         * */
        fun <R> create(threadType: ThreadType): DispatchObservable<R> {
            val threadPair = Threader.getHandlerPairForThreadType(threadType)
            return DispatchObservableImpl(threadPair.first, true)
        }

    }

}