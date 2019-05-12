package com.tonyodev.dispatch

/**
 * Enum that holds the different
 * thread types.
 * */
enum class ThreadType {

    /** Use default IO thread.*/
    IO,

    /** Use default Network thread.*/
    NETWORK,

    /** Use default Background thread.*/
    BACKGROUND,

    /**
     * Use the Secondary Background thread.
     * */
    BACKGROUND_SECONDARY,

    /** Use a new background thread.*/
    NEW,

    /** Uses the user interface thread.*/
    MAIN,

    /** Uses the default test thread handler*/
    TEST;

}