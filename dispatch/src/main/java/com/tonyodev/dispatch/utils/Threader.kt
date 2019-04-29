package com.tonyodev.dispatch.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tonyodev.dispatch.ThreadType

internal object Threader {

    @Volatile
    private var newThreadCount = 0

    val uiHandler = Handler(Looper.getMainLooper())

    val backgroundHandler: Handler = {
        val handlerThread = HandlerThread("dispatcherBackground")
        handlerThread.start()
        Handler(handlerThread.looper)
    }()

    var backgroundSecondaryHandler: Handler? = null
        get() {
            if (field == null) {
                val handlerThread = HandlerThread("dispatcherBackgroundSecondary")
                handlerThread.start()
                field = Handler(handlerThread.looper)
            }
            return field
        }
    var networkHandler: Handler? = null
        get() {
            if (field == null) {
                val handlerThread = HandlerThread("dispatcherNetwork")
                handlerThread.start()
                field = Handler(handlerThread.looper)
            }
            return field
        }
    var ioHandler: Handler? = null
        get() {
            if (field == null) {
                val handlerThread = HandlerThread("dispatcherIO")
                handlerThread.start()
                field = Handler(handlerThread.looper)
            }
            return field
        }

    fun getNewDispatchHandler(name: String? = null): Handler {
        val threadName = if (name == null || name.isEmpty()) {
            "dispatch${++newThreadCount}"
        } else {
            name
        }
        val handlerThread = HandlerThread(threadName)
        handlerThread.start()
        return Handler(handlerThread.looper)
    }

    fun getHandlerPairForThreadType(threadType: ThreadType): Pair<Handler, Boolean> {
        return when(threadType) {
            ThreadType.BACKGROUND -> Pair(backgroundHandler, false)
            ThreadType.BACKGROUND_SECONDARY -> Pair(backgroundSecondaryHandler!!, false)
            ThreadType.IO -> Pair(ioHandler!!, false)
            ThreadType.NETWORK -> Pair(networkHandler!!, false)
            ThreadType.MAIN -> Pair(uiHandler, false)
            ThreadType.NEW -> Pair(getNewDispatchHandler(), true)
        }
    }

}