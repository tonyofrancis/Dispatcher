package com.tonyodev.dispatch.utils

import android.os.Handler
import android.os.Looper
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.AndroidThreadHandler
import com.tonyodev.dispatch.thread.DefaultThreadHandler
import com.tonyodev.dispatch.thread.TestThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandler

internal object Threader {

    @Volatile
    private var newThreadCount = 0

    val uiHandler by lazy { AndroidThreadHandler(Handler(Looper.getMainLooper())) }

    val testHandler by lazy { TestThreadHandler("dispatchTest") }

    val backgroundHandler by lazy { DefaultThreadHandler("dispatchBackground") }

    private val backgroundSecondaryHandler by lazy { DefaultThreadHandler("dispatchBackgroundSecondary") }

    private val networkHandler by lazy { DefaultThreadHandler("dispatchNetwork") }

    private val ioHandler by lazy { DefaultThreadHandler("dispatchIO") }

    fun getNewDispatchHandler(name: String? = null): ThreadHandler {
        val threadName = if (name == null || name.isEmpty()) {
            "dispatch${++newThreadCount}"
        } else {
            name
        }
        return DefaultThreadHandler(threadName)
    }

    private val backgroundThreadPair by lazy { Pair(backgroundHandler, false) }

    private val backgroundSecondaryThreadPair by lazy { Pair(backgroundSecondaryHandler, false) }

    private val networkThreadPair by lazy { Pair(networkHandler, false) }

    private val uiThreadPair by lazy { Pair(uiHandler, false) }

    private val ioThreadPair by lazy { Pair(ioHandler, false) }

    fun getHandlerPairForThreadType(threadType: ThreadType): Pair<ThreadHandler, Boolean> {
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