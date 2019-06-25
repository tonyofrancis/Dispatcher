package com.tonyodev.dispatch

import com.tonyodev.dispatch.thread.DefaultThreadHandlerFactory
import com.tonyodev.dispatch.utils.DefaultLogger
import org.junit.Test

class SettingsTest {

    @Test
    fun testWarningEnabled() {
        val settings = Settings()
        assert(!settings.enableLogWarnings)
        settings.enableLogWarnings = true
        assert(settings.enableLogWarnings)
    }

    @Test
    fun testErrorCallback() {
        val callback = DispatchQueueErrorCallback {  }
        val settings = Settings()
        settings.dispatchQueueErrorCallback = callback
        assert(settings.dispatchQueueErrorCallback == callback)
    }

    @Test
    fun testThreadFactory() {
        val settings = Settings()
        val factory = DefaultThreadHandlerFactory()
        val thread1 = factory.create(ThreadType.BACKGROUND)
        thread1.quit()
        assert(settings.threadHandlerFactory is DefaultThreadHandlerFactory)
        val thread2 = settings.threadHandlerFactory.create(ThreadType.BACKGROUND)
        thread2.quit()
        assert(thread1.threadName == thread2.threadName)
    }

    @Test
    fun testLogger() {
        val settings = Settings()
        assert(settings.logger is DefaultLogger)
    }

}