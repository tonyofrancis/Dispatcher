package com.tonyodev.dispatch

/**
 * Cancel Type used with Dispatch objects
 * that are being managed by an activity/ActivityDispatchController
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