package com.tonyodev.dispatchandroid.thread

import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import com.tonyodev.dispatch.thread.ThreadHandler

/**
 * The default android thread handler that uses the android.os.Handler class to perform work.
 * */
class AndroidThreadHandler (val handler: Handler): ThreadHandler {

    @Volatile
    private var isCancelled = false

    constructor(threadName: String): this({
        val handlerThread = HandlerThread(threadName)
        handlerThread.start()
        Handler(handlerThread.looper)
    }())

    override val threadName: String
        get() {
            return handler.looper.thread.name
        }

    override fun post(runnable: Runnable) {
        if (!isCancelled) {
            handler.post(runnable)
        }
    }

    override fun postDelayed(delayInMilliseconds: Long, runnable: Runnable) {
        if (!isCancelled) {
            handler.postDelayed(runnable, delayInMilliseconds)
        }
    }

    override fun removeCallbacks(runnable: Runnable) {
        if (!isCancelled) {
            handler.removeCallbacksAndMessages(runnable)
        }
    }

    override fun quit() {
        if (!isCancelled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                handler.looper.quitSafely()
            } else {
                handler.looper.quit()
            }
            isCancelled = true
        }
    }

    override val isActive: Boolean
        get() {
            return !isCancelled
        }

    override fun start() {

    }

}