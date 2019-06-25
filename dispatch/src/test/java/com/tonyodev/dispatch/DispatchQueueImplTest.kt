package com.tonyodev.dispatch

import com.tonyodev.dispatch.internals.DispatchQueueImpl
import com.tonyodev.dispatch.internals.DispatchQueueInfo
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.utils.Threader
import org.junit.Test
import java.lang.Exception

class DispatchQueueImplTest {

    @Test
    fun testFields() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        assert(queue.id == 99)
        assert(queue.blockLabel == "test")
        assert(queue.controller == null)
    }

    @Test
    fun testManageController() {
        val controller = DispatchQueueController()
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        queue.managedBy(controller)
        assert(queue.controller == controller)
        assert(controller.getManagedDispatchQueues().contains(queue))
    }

    @Test
    fun testCancelled() {
        val controller = DispatchQueueController()
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        queue.managedBy(controller)
        assert(!queue.isCancelled)
        queue.cancel()
        assert(queue.isCancelled)
    }

    @Test
    fun testExecutePost() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        info.enqueue(queue)
        var data: Any? = null
        queue.post {
            data = 44
        }
        queue.start()
        try {
            Thread.sleep(1000)
        } catch (e: Exception) {

        }
        assert(data is Int && data == 44)
    }

    @Test
    fun testExecuteAsync() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        info.enqueue(queue)
        var data: Any? = null
        queue.async {
            data = 44
        }
        queue.start()
        try {
            Thread.sleep(1000)
        } catch (e: Exception) {

        }
        assert(data is Int && data == 44)
    }

    @Test
    fun testChaining() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        info.enqueue(queue)
        var data: Any? = null
        queue.post {
            44
        }.async {
            data = it * 2
        }
        queue.start()
        try {
            Thread.sleep(1000)
        } catch (e: Exception) {

        }
        assert(data is Int && data == (44 * 2))
    }

    @Test
    fun testDoOnError() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        info.enqueue(queue)
        var data: Any? = null
        queue.post {
            throw Exception("test")
            22
        }.doOnError {
            44
        }.async {
            data = it * 2
        }
        queue.start()
        try {
            Thread.sleep(1000)
        } catch (e: Exception) {

        }
        assert(data is Int && data == (44 * 2))
    }

    @Test
    fun testZip() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        val info2 = DispatchQueueInfo(99, false, handler)
        val queue2 = DispatchQueueImpl<Void?, Void?>("test1", 0,
            null, info2, handler)
        info2.enqueue(queue2)
        info.enqueue(queue)
        var data: Any? = null
        queue.post {
            throw Exception("test")
            22
        }.doOnError {
            12
        }.zip(queue2.async { 44 * 2 })
            .async {
                data = it.second
            }
        queue.start()
        try {
            Thread.sleep(1000)
        } catch (e: Exception) {

        }
        assert(data is Int && data == (44 * 2))
    }

    @Test
    fun testRetry() {
        val handler = Threader.getHandlerThreadInfo(ThreadType.TEST)
        val info = DispatchQueueInfo(99, false, handler)
        val queue = DispatchQueueImpl<Void?, Void?>("test", 0,
            null, info, handler)
        var counter = 0
        info.enqueue(queue)
        var data: Any? = null
        queue.async {
            if (counter == 0) {
                counter = 1
                throw Exception("")
            }
            data = 44
        }.retry(2, 0)
        queue.start()
        try {
            Thread.sleep(1000)
        } catch (e: Exception) {

        }
        assert(data is Int && data == 44)
    }

}