package com.tonyodev.dispatch.thread

/**
 * Wrapper interface used to post work on the underlining thread. The thread can be any class such
 * as Thread, Executor or android.os.Handler. Classes extending this interface needs to handle
 * synchronous access.
 * */
interface ThreadHandler {

    /**
     * The thread name.
     * */
    val threadName: String

    /**
     * Checks if this threadHandler is alive.
     * */
    val isActive: Boolean

    /**
     * Posts the runnable to the thread.
     * @param runnable the runnable to post.
     * */
    fun post(runnable: Runnable)

    /**
     * Posts the runnable to the thread delayed
     * @param delayInMilliseconds the delay in milliseconds
     * @param runnable the runnable to post.
     * */
    fun postDelayed(delayInMilliseconds: Long, runnable: Runnable)

    /**
     * Removes the runnable from the threads processing queue.
     * @param runnable the runnable to remove.
     * */
    fun removeCallbacks(runnable: Runnable)

    /**
     * Terminate the underlining thread that is handling the runnable work.
     * */
    fun quit()

    /**
     * Starts the thread handler if it is not already started.
     * */
    fun start()

}