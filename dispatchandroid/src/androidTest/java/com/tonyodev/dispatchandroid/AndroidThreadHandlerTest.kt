package com.tonyodev.dispatchandroid

import androidx.test.runner.AndroidJUnit4
import com.tonyodev.dispatchandroid.thread.AndroidThreadHandler
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidThreadHandlerTest {

    @Test
    fun testFields() {
        val thread = AndroidThreadHandler("test")
        assert(thread.threadName == "test")
        assert(thread.isActive)
    }

}