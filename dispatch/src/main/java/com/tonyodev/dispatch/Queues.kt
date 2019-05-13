@file:JvmName("DispatchQueues")
package com.tonyodev.dispatch

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on the default background handler/thread.
 * @return new dispatch.
 * */
val backgroundDispatchQueue: Dispatch<Void?>
    get() {
        return Dispatcher.backgroundDispatchQueue
    }

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on the secondary background handler/thread.
 * @return new dispatch.
 * */
val backgroundSecondaryDispatchQueue: Dispatch<Void?>
    get() {
        return Dispatcher.backgroundSecondaryDispatchQueue
    }

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on the default network handler/thread.
 * @return new dispatch.
 * */
val networkDispatchQueue: Dispatch<Void?>
    get() {
        return Dispatcher.networkDispatchQueue
    }

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on the default io handler/thread.
 * @return new dispatch.
 * */
val ioDispatchQueue: Dispatch<Void?>
    get() {
        return Dispatcher.ioDispatchQueue
    }

/**
 * Creates a new test dispatch queue. All async and post run on the same thread this dispatch was created on.
 * Note: Test Dispatch queues do not run with delays.
 * @return test dispatch.
 * */
val testDispatchQueue: Dispatch<Void?>
    get() {
        return Dispatcher.testDispatchQueue
    }

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on a new handler/thread.
 * @return new dispatch.
 * */
val newDispatchQueue: Dispatch<Void?>
    get() {
        return Dispatcher.createDispatchQueue(ThreadType.NEW)
    }