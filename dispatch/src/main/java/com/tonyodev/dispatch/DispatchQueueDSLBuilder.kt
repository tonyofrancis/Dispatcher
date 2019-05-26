package com.tonyodev.dispatch

import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController
import com.tonyodev.dispatch.utils.getNewDispatchId

/***
 * This class holds a dispatch queues settings and code block when created using the Kotlin DSL Language style.
 * */
class DispatchQueueDSLBuilder {

    /**
     * Specifies the delay before the run block is executed. The value must be in milliseconds.
     * */
    var delay = -1L

    /**
     * Loops the run block at the specified interval. The value must be in milliseconds.
     * */
    var interval = -1L

    /**
     * The async or post label.
     * */
    var blockLabel = getNewDispatchId()

    /**
     * The DispatchQueueController that will manage the lifecycle of the async or post block.
     * */
    var controller: DispatchQueueController? = null

    /**
     * The cancel type used by the LifecycleDispatchQueueController to cancel the operation
     * at the appropriate time.
     * */
    var cancelType = CancelType.DESTROYED

    /**
     * Sets the DispatchQueueController and cancelType that will manage the lifecycle of the block.
     * @param dispatchQueueController the DispatchQueueController.
     * @param cancelType the cancelType.
     * */
    fun managedBy(dispatchQueueController: LifecycleDispatchQueueController, cancelType: CancelType) {
        this.controller = dispatchQueueController
        this.cancelType = cancelType
    }

    /**
     * The retry attempt count if an error occurs in the async or post operation.
     * */
    var retryCount = 0

    /**
     * The delay in milliseconds before the next retry attempted after an error occurred.
     * */
    var retryDelay = -1L

    /**
     * Retry the post or async operation if an error occurred.
     * @param retryCount the retry count.
     * @param retryDelay the delay in milliseconds before the next retry attempted.
     * */
    fun retry(retryCount: Int, retryDelay: Long) {
        this.retryCount = retryCount
        this.retryDelay = retryDelay
    }

    /**
     * The block of code to execute on the background or ui thread.
     * */
    var run: (() -> Unit)? = null

    /**
     * Sets the run block.
     * @param block The block of code to execute on the background or ui thread.
     * */
    fun run(block: (() -> Unit)) {
        run = block
    }

    /**
     * The error callback is called if an error occurs. The callback is executed on the main thread.
     * The callback provides the throwable with the error and the second parameter is the block label name.
     * */
    var error: ((DispatchQueueError) -> Unit)? = null

    /**
     * Sets the error callback.
     * @param callback The error callback is called if an error occurs. The callback is executed on the main thread.
     * */
    fun error(callback: ((DispatchQueueError) -> Unit)) {
        error = callback
    }

    internal var cancelled = false

    internal var dispatchQueue: DispatchQueue<*>? = null

    /**
     * Cancels the block.
     * */
    fun cancel() {
        cancelled = true
        dispatchQueue?.cancel()
    }

}