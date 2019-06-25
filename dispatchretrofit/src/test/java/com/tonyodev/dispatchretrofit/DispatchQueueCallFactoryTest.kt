package com.tonyodev.dispatchretrofit

import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.ThreadType
import org.junit.Test

class DispatchQueueCallFactoryTest {

    @Test
    fun testFields() {
        val factory = DispatchQueueCallAdapterFactory.create()
        assert(factory.threadHandler == null)
        assert(factory.onErrorCallback == null)
        val handler = DispatchQueue.globalSettings.threadHandlerFactory.create(ThreadType.TEST)
        val callback = OnErrorCallback { exception, request, response ->  }
        val factory2 = DispatchQueueCallAdapterFactory.create(handler, callback)
        assert(factory2.threadHandler == handler)
        assert(factory2.onErrorCallback == callback)
        val testFactory = DispatchQueueCallAdapterFactory.createTestFactory()
        assert(testFactory.threadHandler?.threadName == handler.threadName)
    }

}