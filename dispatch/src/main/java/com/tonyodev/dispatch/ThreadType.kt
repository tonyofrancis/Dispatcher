package com.tonyodev.dispatch

/**
 * Enum that holds the different
 * thread types.
 * */
enum class ThreadType {

    /** Use Default IO thread.*/
    IO,

    /** Use default Network thread.*/
    NETWORK,

    /** Use default Background thread.*/
    BACKGROUND,

    /** Use a new background thread.*/
    NEW;

}