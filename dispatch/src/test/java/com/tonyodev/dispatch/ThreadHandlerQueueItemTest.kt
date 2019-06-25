package com.tonyodev.dispatch

import com.tonyodev.dispatch.thread.ThreadHandlerQueueItem
import org.junit.Test

class ThreadHandlerQueueItemTest {

    @Test
    fun testValues() {
        val delay = 4L
        val runnable = Runnable {  }
        val pool = ThreadHandlerQueueItem.Pool()
        val item = pool.obtain(delay, runnable)
        assert(item.delay == 4L)
        assert(item.runnable == runnable)
        assert(!item.isProcessing)
    }

    @Test
    fun testPoolSize() {
        val pool = ThreadHandlerQueueItem.Pool()
        assert(pool.size == 0)
        val item = pool.obtain(0, Runnable {  })
        assert(pool.size == 0)
        assert(pool.maxPoolSize == 50)
        pool.recycle(item)
        assert(pool.size == 1)
    }

    @Test
    fun testRecycle() {
        val pool = ThreadHandlerQueueItem.Pool()
        assert(pool.size == 0)
        val item = pool.obtain(0, Runnable {  })
        pool.recycle(item)
        assert(item.runnable == null)
        assert(item.delay == 0L)
    }

}