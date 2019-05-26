package com.tonyodev.dispatch

import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController

/**
 * Internal documentation: Duplicate code because of kotlin bug.
 * todo update when fix become available.
 * */

/**
 * Executes a block of code on the background thread. This method creates a dispatch queue using
 * the Kotlin DSL language style.
 * @param block the block of code that configures and runs the dispatch queue
 * */
fun async(block: DispatchQueueDSLBuilder.()-> Unit) {
    val dslBuilder = DispatchQueueDSLBuilder()
    dslBuilder.block()
    val runBlock = dslBuilder.run
    if (!dslBuilder.cancelled && runBlock != null) {
        //create queue
        val dispatchQueue = if (dslBuilder.interval > 0) {
            DispatchQueue.createIntervalDispatchQueue(dslBuilder.interval)
        } else {
            DispatchQueue.background
        }
        dslBuilder.dispatchQueue = dispatchQueue
        //set run label
        dispatchQueue.setBlockLabel(dslBuilder.blockLabel)

        //set dispatchQueueController
        val dispatchQueueController = dslBuilder.controller
        if (dispatchQueueController is LifecycleDispatchQueueController) {
            dispatchQueue.managedBy(dispatchQueueController, dslBuilder.cancelType)
        } else if (dispatchQueueController != null) {
            dispatchQueue.managedBy(dispatchQueueController)
        }

        // set retry
        if (dslBuilder.retryCount > 0) {
            val retryDelay = if (dslBuilder.retryDelay < 0) 0 else dslBuilder.retryDelay
            dispatchQueue.retry(dslBuilder.retryCount, retryDelay)
        }

        //set delay and execute run
        if (dslBuilder.delay > 0) {
            dispatchQueue.async(dslBuilder.delay) { runBlock.invoke() }
        } else {
            dispatchQueue.async { runBlock.invoke() }
        }

        //set error handler and start
        val errorCallback = dslBuilder.error
        if (errorCallback != null) {
            dispatchQueue.start(DispatchQueueErrorCallback { errorCallback.invoke(it) })
        } else {
            dispatchQueue.start()
        }
    }
}

/**
 * Executes a block of code on the main thread. This method creates a dispatch queue using
 * the Kotlin DSL language style.
 * @param block the block of code that configures and runs the dispatch queue
 * */
fun post(block: DispatchQueueDSLBuilder.()-> Unit) {
    val dslBuilder = DispatchQueueDSLBuilder()
    dslBuilder.block()
    val runBlock = dslBuilder.run
    if (!dslBuilder.cancelled && runBlock != null) {
        //create queue
        val dispatchQueue = if (dslBuilder.interval > 0) {
            DispatchQueue.createIntervalDispatchQueue(dslBuilder.interval)
        } else {
            DispatchQueue.background
        }
        dslBuilder.dispatchQueue = dispatchQueue
        //set run label
        dispatchQueue.setBlockLabel(dslBuilder.blockLabel)

        //set dispatchQueueController
        val dispatchQueueController = dslBuilder.controller
        if (dispatchQueueController is LifecycleDispatchQueueController) {
            dispatchQueue.managedBy(dispatchQueueController, dslBuilder.cancelType)
        } else if (dispatchQueueController != null) {
            dispatchQueue.managedBy(dispatchQueueController)
        }

        // set retry
        if (dslBuilder.retryCount > 0) {
            val retryDelay = if (dslBuilder.retryDelay < 0) 0 else dslBuilder.retryDelay
            dispatchQueue.retry(dslBuilder.retryCount, retryDelay)
        }

        //set delay and execute run
        if (dslBuilder.delay > 0) {
            dispatchQueue.post(dslBuilder.delay) { runBlock.invoke() }
        } else {
            dispatchQueue.post { runBlock.invoke() }
        }

        //set error handler and start
        val errorCallback = dslBuilder.error
        if (errorCallback != null) {
            dispatchQueue.start(DispatchQueueErrorCallback { errorCallback.invoke(it) })
        } else {
            dispatchQueue.start()
        }
    }
}