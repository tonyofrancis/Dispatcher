package com.tonyodev.dispatch.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.tonyodev.dispatch.ThreadType

internal object Threader {

    @Volatile
    private var newThreadCount = 0

    val uiHandler by lazy { Handler(Looper.getMainLooper()) }

    val backgroundHandler: Handler by lazy {
        val handlerThread = HandlerThread("dispatcherBackground")
        handlerThread.start()
        Handler(handlerThread.looper)
    }

    private val backgroundSecondaryHandler: Handler by lazy {
        val handlerThread = HandlerThread("dispatcherBackgroundSecondary")
        handlerThread.start()
        Handler(handlerThread.looper)
    }

    private val networkHandler: Handler by lazy {
        val handlerThread = HandlerThread("dispatcherNetwork")
        handlerThread.start()
        Handler(handlerThread.looper)
    }

    private val ioHandler: Handler by lazy {
        val handlerThread = HandlerThread("dispatcherIO")
        handlerThread.start()
        Handler(handlerThread.looper)
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

    private val backgroundThreadPair: Pair<Handler, Boolean> by lazy { Pair(backgroundHandler, false) }

    private val backgroundSecondaryThreadPair: Pair<Handler, Boolean> by lazy { Pair(backgroundSecondaryHandler, false) }

    private val networkThreadPair: Pair<Handler, Boolean> by lazy { Pair(networkHandler, false) }

    private val uiThreadPair: Pair<Handler, Boolean> by lazy { Pair(uiHandler, false) }

    private val ioThreadPair: Pair<Handler, Boolean> by lazy { Pair(ioHandler, false) }

    fun getHandlerPairForThreadType(threadType: ThreadType): Pair<Handler, Boolean> {
        return when(threadType) {
            ThreadType.BACKGROUND -> backgroundThreadPair
            ThreadType.BACKGROUND_SECONDARY -> backgroundSecondaryThreadPair
            ThreadType.IO -> ioThreadPair
            ThreadType.NETWORK -> networkThreadPair
            ThreadType.MAIN -> uiThreadPair
            ThreadType.NEW -> Pair(getNewDispatchHandler(), true)
        }
    }

}