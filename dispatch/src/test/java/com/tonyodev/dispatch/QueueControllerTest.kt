package com.tonyodev.dispatch

import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import org.junit.Test

class QueueControllerTest {

    @Test
    fun testManage() {
        val controller = DispatchQueueController()
        val queue = DispatchQueue.createDispatchQueue()
        controller.manage(queue)
        assert(controller.getManagedDispatchQueues().size == 1)
        assert(controller.getManagedDispatchQueues().first() == queue)
        assert(!queue.isCancelled)
    }

    @Test
    fun testUnManage() {
        val controller = DispatchQueueController()
        val queue = DispatchQueue.createDispatchQueue()
        controller.manage(queue)
        assert(controller.getManagedDispatchQueues().size == 1)
        assert(controller.getManagedDispatchQueues().first() == queue)
        controller.unmanage(queue)
        assert(controller.getManagedDispatchQueues().isEmpty())
        assert(!queue.isCancelled)
    }

    @Test
    fun testCancelId() {
        val controller = DispatchQueueController()
        val queue = DispatchQueue.createDispatchQueue()
        controller.manage(queue)
        controller.cancelDispatchQueues(queue.id)
        assert(controller.getManagedDispatchQueues().isEmpty())
        assert(queue.isCancelled)
    }

    @Test
    fun testCancel() {
        val controller = DispatchQueueController()
        val queue = DispatchQueue.createDispatchQueue()
        controller.manage(queue)
        controller.cancelDispatchQueues(queue)
        assert(controller.getManagedDispatchQueues().isEmpty())
        assert(queue.isCancelled)
    }

    @Test
    fun testLifecyclePaused() {
        val controller = LifecycleDispatchQueueController()
        val queue = DispatchQueue.createDispatchQueue()
        controller.manage(queue, CancelType.PAUSED)
        assert(controller.getManagedPausedDispatchQueues().isNotEmpty())
        assert(controller.getManagedDestroyedDispatchQueues().isNotEmpty())
        assert(controller.getManagedStoppedDispatchQueues().isEmpty())
        assert(controller.getManagedDispatchQueues().size == 1)
        assert(!queue.isCancelled)
    }

    @Test
    fun testLifecycleStopped() {
        val controller = LifecycleDispatchQueueController()
        val queue = DispatchQueue.createDispatchQueue()
        controller.manage(queue, CancelType.STOPPED)
        assert(controller.getManagedStoppedDispatchQueues().isNotEmpty())
        assert(controller.getManagedDestroyedDispatchQueues().isNotEmpty())
        assert(controller.getManagedPausedDispatchQueues().isEmpty())
        assert(controller.getManagedDispatchQueues().size == 1)
        assert(!queue.isCancelled)
    }

}