package com.tonyodev.dispatch.utils


import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.ThreadHandler

internal object Threader {

    private val uiHandler by lazy { DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.MAIN) }

    private val testHandler by lazy { DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.TEST) }

    private val backgroundHandler by lazy { DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.BACKGROUND) }

    private val ioHandler by lazy { DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.IO) }

    private val networkHandler by lazy { DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.NETWORK) }

    private val backgroundThreadHandlerInfo by lazy { ThreadHandlerInfo(backgroundHandler, false) }

    private val uiThreadHandlerInfo by lazy { ThreadHandlerInfo(uiHandler, false) }

    private val networkThreadHandlerInfo by lazy { ThreadHandlerInfo(networkHandler, false) }

    private val ioThreadHandlerInfo by lazy { ThreadHandlerInfo(ioHandler, false) }

    private val testThreadHandlerInfo by lazy { ThreadHandlerInfo(testHandler, false) }

    fun getHandlerThreadInfo(threadType: ThreadType): ThreadHandlerInfo {
        return when(threadType) {
            ThreadType.BACKGROUND -> backgroundThreadHandlerInfo
            ThreadType.IO -> ioThreadHandlerInfo
            ThreadType.NETWORK -> networkThreadHandlerInfo
            ThreadType.MAIN -> uiThreadHandlerInfo
            ThreadType.TEST -> testThreadHandlerInfo
            ThreadType.NEW -> ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.NEW), true)
        }
    }

    fun getHandlerThreadInfo(threadName: String): ThreadHandlerInfo {
        return ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(threadName), true)
    }

    data class ThreadHandlerInfo(val threadHandler: ThreadHandler, val closeThreadHandler: Boolean) {
        val threadName: String
            get() {
                return threadHandler.threadName
            }
    }

}