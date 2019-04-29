package com.tonyodev.dispatch.queuecontroller

/**
 * Cancel Type used with a Dispatch queues
 * that are being managed by an ActivityDispatchQueueController
 * */
enum class CancelType {

    /**
     * Reflects the activity's paused state.
     * */
    PAUSED,

    /**
     * Reflects the activity's stopped state.
     * */
    STOPPED,

    /**
     * Reflects the activity's destroyed state.
     * */
    DESTROYED;

}