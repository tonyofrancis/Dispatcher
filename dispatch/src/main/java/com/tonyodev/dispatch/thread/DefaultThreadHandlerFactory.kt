package com.tonyodev.dispatch.thread

import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.utils.*

/**
 * The default ThreadHandler Factory used by the library.
 * */
class DefaultThreadHandlerFactory: ThreadHandlerFactory {

    @Volatile
    private var newThreadCount = 0

    override fun create(threadType: ThreadType): ThreadHandler {
        val threadHandler = when(threadType) {
            ThreadType.BACKGROUND -> DefaultThreadHandler(THREAD_BACKGROUND)
            ThreadType.BACKGROUND_SECONDARY -> DefaultThreadHandler(THREAD_BACKGROUND_SECONDARY)
            ThreadType.NETWORK -> DefaultThreadHandler(THREAD_NETWORK)
            ThreadType.IO -> DefaultThreadHandler(THREAD_IO)
            ThreadType.NEW -> getNewDispatchHandler()
            ThreadType.MAIN -> DefaultThreadHandler(THREAD_MAIN_NO_UI)
            ThreadType.TEST -> TestThreadHandler(THREAD_TEST)
        }
        if (!threadHandler.isActive) {
            threadHandler.start()
        }
        return threadHandler
    }

    override fun create(threadName: String?): ThreadHandler {
        val threadHandler = getNewDispatchHandler(threadName)
        threadHandler.start()
        return threadHandler
    }

    private fun getNewDispatchHandler(name: String? = null): ThreadHandler {
        val threadName = if (name == null || name.isEmpty()) {
            "dispatch${++newThreadCount}"
        } else {
            name
        }
        val threadHandler = DefaultThreadHandler(threadName)
        threadHandler.start()
        return threadHandler
    }

}