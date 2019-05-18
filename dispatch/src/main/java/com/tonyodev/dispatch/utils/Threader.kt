package com.tonyodev.dispatch.utils


import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.ThreadHandler

internal object Threader {

    private val uiHandler by lazy { DispatchQueue.threadHandlerFactory.create(ThreadType.MAIN) }

    private val testHandler by lazy { DispatchQueue.threadHandlerFactory.create(ThreadType.TEST) }

    private val backgroundHandler by lazy { DispatchQueue.threadHandlerFactory.create(ThreadType.BACKGROUND) }

    private val ioHandler by lazy { DispatchQueue.threadHandlerFactory.create(ThreadType.IO) }

    private val backgroundThreadHandlerInfo by lazy { ThreadHandlerInfo(backgroundHandler, false) }

    private val uiThreadHandlerInfo by lazy { ThreadHandlerInfo(uiHandler, false) }

    private val ioThreadHandlerInfo by lazy { ThreadHandlerInfo(ioHandler, false) }

    private val testThreadHandlerInfo by lazy { ThreadHandlerInfo(testHandler, false) }

    fun getHandlerThreadInfo(threadType: ThreadType): ThreadHandlerInfo {
        return when(threadType) {
            ThreadType.BACKGROUND -> backgroundThreadHandlerInfo
            ThreadType.IO -> ioThreadHandlerInfo
            ThreadType.MAIN -> uiThreadHandlerInfo
            ThreadType.TEST -> testThreadHandlerInfo
            ThreadType.NEW -> ThreadHandlerInfo(DispatchQueue.threadHandlerFactory.create(ThreadType.NEW), true)
        }
    }

    fun getHandlerThreadInfo(threadName: String): ThreadHandlerInfo {
        return ThreadHandlerInfo(DispatchQueue.threadHandlerFactory.create(threadName), true)
    }

    data class ThreadHandlerInfo(val threadHandler: ThreadHandler, val closeThreadHandler: Boolean) {
        val threadName: String
            get() {
                return threadHandler.threadName
            }
    }

}