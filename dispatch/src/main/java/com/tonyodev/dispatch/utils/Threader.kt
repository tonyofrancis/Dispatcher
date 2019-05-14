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

    private val backgroundThreadHandlerInfo by lazy { ThreadHandlerInfo(backgroundHandler, false) }

    private val backgroundSecondaryThreadHandlerInfo by lazy { ThreadHandlerInfo(backgroundSecondaryHandler, false) }

    private val networkThreadHandlerInfo by lazy { ThreadHandlerInfo(networkHandler, false) }

    private val uiThreadHandlerInfo by lazy { ThreadHandlerInfo(uiHandler, false) }

    private val ioThreadHandlerInfo by lazy { ThreadHandlerInfo(ioHandler, false) }

    private val testThreadHandlerInfo by lazy { ThreadHandlerInfo(testHandler, false) }

    fun getHandlerThreadInfo(threadType: ThreadType): ThreadHandlerInfo {
        return when(threadType) {
            ThreadType.BACKGROUND -> backgroundThreadHandlerInfo
            ThreadType.BACKGROUND_SECONDARY -> backgroundSecondaryThreadHandlerInfo
            ThreadType.IO -> ioThreadHandlerInfo
            ThreadType.NETWORK -> networkThreadHandlerInfo
            ThreadType.MAIN -> uiThreadHandlerInfo
            ThreadType.TEST -> testThreadHandlerInfo
            ThreadType.NEW -> ThreadHandlerInfo(Dispatcher.threadHandlerFactory.create(ThreadType.NEW), true)
        }
    }

    fun getHandlerThreadInfo(threadName: String): ThreadHandlerInfo {
        return ThreadHandlerInfo(Dispatcher.threadHandlerFactory.create(threadName), true)
    }

    data class ThreadHandlerInfo(val threadHandler: ThreadHandler, val closeThreadHandler: Boolean) {
        val threadName: String
            get() {
                return threadHandler.threadName
            }
    }

}