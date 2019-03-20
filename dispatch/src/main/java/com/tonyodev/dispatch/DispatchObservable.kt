package com.tonyodev.dispatch

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

}