package com.tonyodev.dispatch.thread

/** The default test thread handler */
class TestThreadHandler(override val threadName: String): ThreadHandler {

    override fun post(runnable: Runnable) {
        synchronized(this) {
            runnable.run()
        }
    }

    override fun postDelayed(delayInMilliseconds: Long, runnable: Runnable) {
        synchronized(this) {
            runnable.run()
        }
    }

    override fun removeCallbacks(runnable: Runnable) {

    }

    override fun quit() {

    }

}