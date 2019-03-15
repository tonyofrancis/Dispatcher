package com.tonyodev.dispatch

/**
 * Interface that allows for attaching of dispatch observers.
 * */
interface DispatchObservable<R> {

    /**
     * Adds a dispatch observer to the dispatch.
     * @param dispatchObserver the observer.
     * @return the dispatch.
     * */
    fun addObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R>

    /**
     * Adds a list of dispatch observers to the dispatch.
     * @param dispatchObservers the list of observers.
     * @return the dispatch.
     * */
    fun addObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R>

    /**
     * Removes a dispatch observer from the dispatch.
     * @param dispatchObserver the observer to be removed.
     * @return the dispatch.
     * */
    fun removeObserver(dispatchObserver: DispatchObserver<R>): Dispatch<R>

    /**
     * Remove a list of dispatch observers from the dispatch.
     * @param dispatchObservers the list of observers to be removed.
     * @return the dispatch.
     * */
    fun removeObservers(dispatchObservers: List<DispatchObserver<R>>): Dispatch<R>

}