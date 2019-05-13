package com.tonyodev.dispatch.queuecontroller

/**
 * Cancel Type used with a Dispatch queues
 * that are being managed by a LifecycleDispatchQueueController
 * */
enum class CancelType {

    /**
     * Reflects the paused state.
     * */
    PAUSED,

    /**
     * Reflects the stopped state.
     * */
    STOPPED,

    /**
     * Reflects the destroyed state.
     * */
    DESTROYED;

}