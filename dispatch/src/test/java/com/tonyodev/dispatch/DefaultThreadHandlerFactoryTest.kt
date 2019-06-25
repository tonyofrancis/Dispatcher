package com.tonyodev.dispatch

import com.tonyodev.dispatch.thread.DefaultThreadHandlerFactory
import com.tonyodev.dispatch.utils.THREAD_BACKGROUND
import com.tonyodev.dispatch.utils.THREAD_MAIN_NO_UI
import org.junit.Test

class DefaultThreadHandlerFactoryTest {

    private val defaultThreadHandlerFactory = DefaultThreadHandlerFactory()

    @Test
    fun testCreateBackgroundThread() {
        val thread = defaultThreadHandlerFactory.create(ThreadType.BACKGROUND)
        assert(thread.threadName == THREAD_BACKGROUND)
        thread.quit()
        assert(!thread.isActive)
    }

    @Test
    fun testCreateMainThread() {
        val thread = defaultThreadHandlerFactory.create(ThreadType.MAIN)
        assert(thread.threadName == THREAD_MAIN_NO_UI)
        thread.quit()
        assert(!thread.isActive)
    }

    @Test
    fun testCreatedThreadActive() {
        val thread = defaultThreadHandlerFactory.create(ThreadType.BACKGROUND)
        assert(thread.isActive)
        thread.quit()
        assert(!thread.isActive)
    }

    @Test
    fun testCreatedThreadByName() {
        val name = "sampleThread"
        val thread = defaultThreadHandlerFactory.create(name)
        assert(thread.threadName == name)
        thread.quit()
        assert(!thread.isActive)
    }

}