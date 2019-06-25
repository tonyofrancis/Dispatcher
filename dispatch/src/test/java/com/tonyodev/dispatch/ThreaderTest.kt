package com.tonyodev.dispatch

import com.tonyodev.dispatch.utils.THREAD_BACKGROUND
import com.tonyodev.dispatch.utils.THREAD_MAIN_NO_UI
import com.tonyodev.dispatch.utils.Threader
import org.junit.Test

class ThreaderTest {

    @Test
    fun getBackgroundThreadInfo() {
        val info = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND)
        assert(!info.closeThreadHandler)
        assert(info.threadName == THREAD_BACKGROUND)
        val info2 = Threader.getHandlerThreadInfo(ThreadType.BACKGROUND)
        assert(info == info2)
    }

    @Test
    fun getMainThreadInfo() {
        val info = Threader.getHandlerThreadInfo(ThreadType.MAIN)
        assert(!info.closeThreadHandler)
        assert(info.threadName == THREAD_MAIN_NO_UI)
        val info2 = Threader.getHandlerThreadInfo(ThreadType.MAIN)
        assert(info == info2)
    }

    @Test
    fun getNewThreadInfo() {
        val name = "newThread"
        val info = Threader.getHandlerThreadInfo(name)
        assert(info.threadName == name)
        assert(info.closeThreadHandler)
        val info2 = Threader.getHandlerThreadInfo(name)
        assert(info2.threadName == name)
        assert(info != info2)
        assert(info2.closeThreadHandler)
    }

}