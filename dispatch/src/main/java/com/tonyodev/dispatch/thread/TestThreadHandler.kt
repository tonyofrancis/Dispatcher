package com.tonyodev.dispatch.thread

/** The default test thread handler */
class TestThreadHandler(override val threadName: String): ThreadHandler {

    override fun post(runnable: Runnable) {
        postDelayed(0, runnable)
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

    override val isActive: Boolean = true

    override fun start() {

    }

}