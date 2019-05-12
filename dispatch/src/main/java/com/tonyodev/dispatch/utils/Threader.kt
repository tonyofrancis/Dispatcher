package com.tonyodev.dispatch.utils

import com.tonyodev.dispatch.Dispatcher
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.ThreadHandler

internal object Threader {

    private val uiHandler by lazy { Dispatcher.threadHandlerFactory.create(ThreadType.MAIN) }

    private val testHandler by lazy { Dispatcher.threadHandlerFactory.create(ThreadType.TEST) }

    private val backgroundHandler by lazy { Dispatcher.threadHandlerFactory.create(ThreadType.BACKGROUND) }

    private val backgroundSecondaryHandler by lazy { Dispatcher.threadHandlerFactory.create(ThreadType.BACKGROUND_SECONDARY) }

    private val networkHandler by lazy { Dispatcher.threadHandlerFactory.create(ThreadType.NETWORK) }

    private val ioHandler by lazy { Dispatcher.threadHandlerFactory.create(ThreadType.IO) }

    private val backgroundThreadPair by lazy { ThreadHandlerInfo(backgroundHandler, false) }

    private val backgroundSecondaryThreadPair by lazy { ThreadHandlerInfo(backgroundSecondaryHandler, false) }

    private val networkThreadPair by lazy { ThreadHandlerInfo(networkHandler, false) }

    private val uiThreadPair by lazy { ThreadHandlerInfo(uiHandler, false) }

    private val ioThreadPair by lazy { ThreadHandlerInfo(ioHandler, false) }

    private val testThreadPair by lazy { ThreadHandlerInfo(testHandler, false) }

    fun getHandlerThreadInfo(threadType: ThreadType): ThreadHandlerInfo {
        return when(threadType) {
            ThreadType.BACKGROUND -> backgroundThreadPair
            ThreadType.BACKGROUND_SECONDARY -> backgroundSecondaryThreadPair
            ThreadType.IO -> ioThreadPair
            ThreadType.NETWORK -> networkThreadPair
            ThreadType.MAIN -> uiThreadPair
            ThreadType.TEST -> testThreadPair
            ThreadType.NEW -> ThreadHandlerInfo(Dispatcher.threadHandlerFactory.create(ThreadType.NEW), true)
        }
    }

    fun getHandlerThreadInfo(threadName: String): ThreadHandlerInfo {
        return ThreadHandlerInfo(Dispatcher.threadHandlerFactory.create(threadName), true)
    }

    data class ThreadHandlerInfo(val threadHandler: ThreadHandler, val closeHandler: Boolean) {
        val threadName: String
            get() {
                return threadHandler.threadName
            }
    }

}