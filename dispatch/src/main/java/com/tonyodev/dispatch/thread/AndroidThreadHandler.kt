package com.tonyodev.dispatch.thread

import android.os.Build
import android.os.Handler
import android.os.HandlerThread

class AndroidThreadHandler(val handler: Handler): ThreadHandler {

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
        handler.post(runnable)
    }

    override fun postDelayed(delayInMilliseconds: Long, runnable: Runnable) {
        handler.postDelayed(runnable, delayInMilliseconds)
    }

    override fun removeCallbacks(runnable: Runnable) {
        handler.removeCallbacksAndMessages(runnable)
    }

    override fun quit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            handler.looper.quitSafely()
        } else {
            handler.looper.quit()
        }
    }

}