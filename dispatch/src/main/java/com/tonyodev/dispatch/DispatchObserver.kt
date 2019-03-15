package com.tonyodev.dispatch

/**
 * DispatchObservers listen for data changes on a dispatch object.
 * */
@FunctionalInterface
interface DispatchObserver<R> {

    /**
     * Method called when the data on the observing object has changed.
     * Called on the main thread.
     * @param data the data.
     * */
    fun onChanged(data: R)

}