package com.tonyodev.dispatchandroid

import com.tonyodev.dispatch.Dispatcher
import com.tonyodev.dispatchandroid.thread.AndroidThreadHandlerFactory
import com.tonyodev.dispatchandroid.utils.AndroidLogger

/**
 * Configures the Dispatcher to use the Android classes instead of the default.
 * */
class AndroidDispatcher private constructor() {

    companion object {

        /**
         * Configures the Dispatcher to use the Android classes instead of the default.
         * */
        @JvmStatic
        fun init() {
            Dispatcher.logger = AndroidLogger()
            Dispatcher.threadHandlerFactory = AndroidThreadHandlerFactory()
        }

    }

}