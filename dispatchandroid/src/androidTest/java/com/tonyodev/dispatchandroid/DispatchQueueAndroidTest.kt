package com.tonyodev.dispatchandroid

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatchandroid.thread.AndroidThreadHandlerFactory
import com.tonyodev.dispatchandroid.utils.AndroidLogger
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DispatchQueueAndroidTest {

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.tonyodev.dispatchandroid.test", appContext.packageName)
    }

    @Test
    fun testInit() {
        initAndroidDispatchQueues()
        assert(DispatchQueue.globalSettings.logger is AndroidLogger)
        assert(DispatchQueue.globalSettings.threadHandlerFactory is AndroidThreadHandlerFactory)
    }

}