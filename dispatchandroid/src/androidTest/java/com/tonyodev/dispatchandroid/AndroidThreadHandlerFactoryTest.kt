package com.tonyodev.dispatchandroid

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.DefaultThreadHandlerFactory
import com.tonyodev.dispatch.utils.THREAD_BACKGROUND
import com.tonyodev.dispatch.utils.THREAD_MAIN_NO_UI
import com.tonyodev.dispatchandroid.thread.AndroidThreadHandlerFactory
import junit.framework.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidThreadHandlerFactoryTest {

    private lateinit var androidThreadHandlerFactory: AndroidThreadHandlerFactory

    @Before
    fun testInit() {
        androidThreadHandlerFactory = AndroidThreadHandlerFactory()
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        Assert.assertEquals("com.tonyodev.dispatchandroid.test", appContext.packageName)
    }

    @Test
    fun testCreateBackgroundThread() {
        val thread = androidThreadHandlerFactory.create(ThreadType.BACKGROUND)
        assert(thread.threadName == THREAD_BACKGROUND)
        thread.quit()
        assert(!thread.isActive)
    }

    @Test
    fun testCreateMainThread() {
        val thread = androidThreadHandlerFactory.create(ThreadType.MAIN)
        assert(thread.threadName == "main")
        thread.quit()
        assert(!thread.isActive)
    }

    @Test
    fun testCreatedThreadActive() {
        val thread = androidThreadHandlerFactory.create(ThreadType.BACKGROUND)
        assert(thread.isActive)
        thread.quit()
        assert(!thread.isActive)
    }

    @Test
    fun testCreatedThreadByName() {
        val name = "sampleThread"
        val thread = androidThreadHandlerFactory.create(name)
        assert(thread.threadName == name)
        thread.quit()
        assert(!thread.isActive)
    }

}