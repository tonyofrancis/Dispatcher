package com.tonyodev.dispatch

import com.tonyodev.dispatch.thread.DefaultThreadHandlerFactory
import com.tonyodev.dispatch.thread.ThreadHandlerFactory
import com.tonyodev.dispatch.utils.DefaultLogger
import com.tonyodev.dispatch.utils.Logger

/**
 * Class that holds the DispatchQueue's global Settings.
 * */
class Settings {

    /**
     * Enable or disable log warnings by the library.
     * */
    var enableLogWarnings = false

    /**
     * Sets the global error handler for Dispatch objects. This error handler is called only
     * if a dispatch queue does not handler its errors. The error handler is called on the main thread.
     * */
    var dispatchQueueErrorCallback: DispatchQueueErrorCallback? = null

    /**
     * Sets the global thread handler factory that is responsible for creating thread handlers that the dispatch queues
     * will use to process work in the background.
     * */
    var threadHandlerFactory: ThreadHandlerFactory = DefaultThreadHandlerFactory()

    /**
     * Sets the logger uses by the library to report warnings and messages.
     * */
    var logger: Logger = DefaultLogger()

}