package com.tonyodev.dispatch

/**
 * DispatchQueueObservers listen for data changes on a dispatch queue object.
 * */
@FunctionalInterface
interface DispatchQueueObserver<T> {

    /**
     * Method called when the data on the observing object has changed.
     * Called on the thread that belongs to the dispatch queue object. If post
     * was called the onChanged will be called on the main thread. If async
     * was called the onChange is called on the thread that the dispatch queue object uses.
     * @param t the data.
     * */
    fun onChanged(t: T)

}