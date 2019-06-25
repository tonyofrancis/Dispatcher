package com.tonyodev.dispatch

import com.tonyodev.dispatch.internals.DispatchQueueImpl
import com.tonyodev.dispatch.internals.DispatchQueueInfo
import com.tonyodev.dispatch.utils.Threader
import org.junit.Test

class DispatchQueueInfoTest {

    @Test
    fun testFieldValue() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        assert(info.queueId == 99)
        assert(!info.isIntervalDispatch)
        assert(info.threadHandlerInfo == handler)
        assert(!info.isCancelled)
        assert(!info.isStarted)
    }

    @Test
    fun testEnqueue() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void>("test", 0,
            null, info, handler)
        info.enqueue(queue)
        assert(info.rootDispatchQueue == queue)
        assert(info.endDispatchQueue == queue)
        val queue1 = DispatchQueueImpl<Void?, Void>("test2", 0,
            null, info, handler)
        info.enqueue(queue1)
        assert(info.rootDispatchQueue == queue)
        assert(info.endDispatchQueue == queue1)
    }

    @Test
    fun testCanPerformOperations() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        assert(info.canPerformOperations())
        info.isCancelled = true
        assert(!info.canPerformOperations())
    }

}