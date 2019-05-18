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
            ThreadType.IO -> DefaultThreadHandler(THREAD_IO)
            ThreadType.NEW -> getNewThreadHandler()
            ThreadType.MAIN -> DefaultThreadHandler(THREAD_MAIN_NO_UI)
            ThreadType.TEST -> TestThreadHandler(THREAD_TEST)
        }
        startThreadHandlerIfNotActive(threadHandler)
        return threadHandler
    }

    override fun create(threadName: String?): ThreadHandler {
        return getNewThreadHandler(threadName)
    }

    private fun getNewThreadHandler(name: String? = null): ThreadHandler {
        val threadName = if (name == null || name.isEmpty()) {
            "DispatchQueue${++newThreadCount}"
        } else {
            name
        }
        val threadHandler = DefaultThreadHandler(threadName)
        startThreadHandlerIfNotActive(threadHandler)
        return threadHandler
    }

}