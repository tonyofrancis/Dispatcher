package com.tonyodev.dispatch.thread

/** The default test thread handler */
class TestThreadHandler(override val threadName: String): ThreadHandler {

    override fun post(runnable: Runnable) {
        postDelayed(0, runnable)
    }

    override fun postDelayed(delayInMilliseconds: Long, runnable: Runnable) {
        synchronized(this) {
            if (isActive) {
                runnable.run()
            }
        }
    }

    override fun removeCallbacks(runnable: Runnable) {

    }

    override fun quit() {
        synchronized(this) {
            isActive = false
        }
    }

    override var isActive: Boolean = false

    override fun start() {
        synchronized(this) {
            isActive = true
        }
    }

}