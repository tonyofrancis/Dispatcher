package com.tonyodev.dispatch

/**
 * Enum that holds the different thread types.
 * */
enum class ThreadType {

    /** Use default IO thread.*/
    IO,

    /** Use default Background thread.*/
    BACKGROUND,

    /** Use default Network thread.*/
    NETWORK,

    /** Use default Computation thread. Use this thread for long computation work*/
    COMPUTATION,

    /** Use a new background thread.*/
    NEW,

    /** On Android Uses the user interface thread called main. On other JVMs a thread called dispatchMain is created by default.*/
    MAIN,

    /** Uses the default test thread handler*/
    TEST;

}