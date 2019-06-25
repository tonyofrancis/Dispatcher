package com.tonyodev.dispatch.utils


import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.ThreadHandler

internal object Threader {

    private val backgroundThreadHandlerInfo by lazy { ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.BACKGROUND), false) }

    private val uiThreadHandlerInfo by lazy { ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.MAIN), false) }

    private val networkThreadHandlerInfo by lazy { ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.NETWORK), false) }

    private val ioThreadHandlerInfo by lazy { ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.IO), false) }

    private val computationThreadHandlerInfo by lazy { ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.COMPUTATION), false) }

    private val testThreadHandlerInfo by lazy { ThreadHandlerInfo(DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.TEST), false) }

    fun getHandlerThreadInfo(threadType: ThreadType): ThreadHandlerInfo {
        return when(threadType) {
            ThreadType.BACKGROUND -> backgroundThreadHandlerInfo
            ThreadType.IO -> ioThreadHandlerInfo
            ThreadType.NETWORK -> networkThreadHandlerInfo
            ThreadType.COMPUTATION -> computationThreadHandlerInfo
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
                return threadHandler.name
            }
    }

}