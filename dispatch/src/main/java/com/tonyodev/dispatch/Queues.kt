@file:JvmName("DispatchQueues")
package com.tonyodev.dispatch

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on the default background handler/thread.
 * @return new dispatch queue.
 * */
val backgroundDispatchQueue: DispatchQueue<Void?>
    get() {
        return Dispatcher.background
    }

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on the secondary background handler/thread.
 * @return new dispatch queue.
 * */
val backgroundSecondaryDispatchQueue: DispatchQueue<Void?>
    get() {
        return Dispatcher.backgroundSecondary
    }

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on the default io handler/thread.
 * @return new dispatch queue.
 * */
val ioDispatchQueue: DispatchQueue<Void?>
    get() {
        return Dispatcher.io
    }

/**
 * Creates a new test dispatch queue. All async and post run on the same thread this dispatch was created on.
 * Note: Test Dispatch queues do not run with delays.
 * @return test dispatch queue.
 * */
val testDispatchQueue: DispatchQueue<Void?>
    get() {
        return Dispatcher.test
    }

/**
 * Creates a new dispatch queue that can be used to post work on the main thread or do work in the background.
 * The dispatch queue operates on a new handler/thread.
 * @return new dispatch queue.
 * */
val newDispatchQueue: DispatchQueue<Void?>
    get() {
        return Dispatcher.createDispatchQueue(ThreadType.NEW)
    }

/**
 * Creates a new main dispatch queue. All async and post run on the main thread.
 * @return main dispatch queue.
 * */
val mainDispatchQueue: DispatchQueue<Void?>
    get() {
        return Dispatcher.main
    }