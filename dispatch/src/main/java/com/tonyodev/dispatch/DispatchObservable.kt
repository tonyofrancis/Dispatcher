package com.tonyodev.dispatch

/**
 * Interface that allows for the attaching of dispatch observers.
 * */
interface DispatchObservable<R, T> {

    /**
     * Adds a dispatch observer.
     * @param dispatchObserver the observer.
     * @return the dispatch.
     * */
    fun addObserver(dispatchObserver: DispatchObserver<R>): T

    /**
     * Adds a list of dispatch observers.
     * @param dispatchObservers the list of observers.
     * @return the dispatch.
     * */
    fun addObservers(dispatchObservers: List<DispatchObserver<R>>): T

    /**
     * Removes a dispatch observer.
     * @param dispatchObserver the observer to be removed.
     * @return the dispatch.
     * */
    fun removeObserver(dispatchObserver: DispatchObserver<R>): T

    /**
     * Remove a list of dispatch observers.
     * @param dispatchObservers the list of observers to be removed.
     * @return the dispatch.
     * */
    fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): T

}