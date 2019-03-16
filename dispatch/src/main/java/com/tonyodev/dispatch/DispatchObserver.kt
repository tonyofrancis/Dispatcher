package com.tonyodev.dispatch

/**
 * DispatchObservers listen for data changes on a dispatch object.
 * */
@FunctionalInterface
interface DispatchObserver<R> {

    /**
     * Method called when the data on the observing object has changed.
     * Called on the thread that belongs to the dispatch object. If postMain
     * was called the onChanged will be called on the main thread. If doWork
     * was called the onChange is called on the thread that the dispatch object uses.
     * @param data the data.
     * */
    fun onChanged(data: R)

}