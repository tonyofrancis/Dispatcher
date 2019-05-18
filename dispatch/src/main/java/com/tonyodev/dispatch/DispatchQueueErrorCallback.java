package com.tonyodev.dispatch;


/**
 * Interface used by the DispatchQueue to rely errors.
 * */
@FunctionalInterface
public interface DispatchQueueErrorCallback {

    /**
     * Called when an error occurs inside a dispatchQueue.
     * @param dispatchQueueError the error.
     * */
    void onError(DispatchQueueError dispatchQueueError);

}