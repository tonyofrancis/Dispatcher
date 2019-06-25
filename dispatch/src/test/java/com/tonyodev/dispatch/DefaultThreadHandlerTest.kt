package com.tonyodev.dispatch

import com.tonyodev.dispatch.thread.DefaultThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandlerQueueItem
import com.tonyodev.dispatch.thread.findQueueMinIndex
import org.junit.Test

class DefaultThreadHandlerTest {

    @Test
    fun testName() {
        val name = "testThread"
        val thread = DefaultThreadHandler(name)
        assert(thread.threadName == name)
        assert(thread.name == name)
    }

    @Test
    fun testStart() {
        val name = "testThread"
        val thread = DefaultThreadHandler(name)
        assert(!thread.isActive)
        assert(!thread.isAlive)
        thread.start()
        assert(thread.isAlive)
        assert(thread.isActive)
    }

    @Test
    fun testMinQueueIndex() {
        val pool = ThreadHandlerQueueItem.Pool()
        val item = pool.obtain(4, Runnable {  })
        val item2 = pool.obtain(0, Runnable {  })
        val item3 = pool.obtain(0, Runnable {  })
        val item4 = pool.obtain(6, Runnable {  })
        val queue = listOf(item, item2, item3, item4)
        val minQueueIndexPair = DefaultThreadHandler.MinQueueIndexPair()
        findQueueMinIndex(queue, minQueueIndexPair)
        assert(minQueueIndexPair.index == 1)
        assert(minQueueIndexPair.waitTime > -1)
    }

}