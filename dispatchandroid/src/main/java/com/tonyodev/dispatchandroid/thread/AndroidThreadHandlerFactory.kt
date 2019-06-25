package com.tonyodev.dispatchandroid.thread

import android.os.Handler
import android.os.Looper
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.DefaultThreadHandler
import com.tonyodev.dispatch.thread.TestThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandlerFactory
import com.tonyodev.dispatch.utils.*

/**
 * The Default Android Handler based ThreadHandler Factory used by the library.
 * */
open class AndroidThreadHandlerFactory: ThreadHandlerFactory {

    @Volatile
    private var newThreadCount = 0

    override fun create(threadType: ThreadType): ThreadHandler {
        val threadHandler = when(threadType) {
            ThreadType.BACKGROUND -> DefaultThreadHandler(THREAD_BACKGROUND)
            ThreadType.IO -> DefaultThreadHandler(THREAD_IO)
            ThreadType.NETWORK -> DefaultThreadHandler(THREAD_NETWORK)
            ThreadType.COMPUTATION -> DefaultThreadHandler(THREAD_COMPUTATION)
            ThreadType.NEW -> getNewDispatchQueueHandler()
            ThreadType.MAIN -> AndroidThreadHandler(Handler(Looper.getMainLooper()))
            ThreadType.TEST -> TestThreadHandler(THREAD_TEST)
        }
        if (!threadHandler.isActive) {
            threadHandler.start()
        }
        return threadHandler
    }

    override fun create(threadName: String?): ThreadHandler {
        return getNewDispatchQueueHandler(threadName)
    }

    private fun getNewDispatchQueueHandler(name: String? = null): ThreadHandler {
        val threadName = if (name == null || name.isEmpty()) {
            "DispatchQueue${++newThreadCount}"
        } else {
            name
        }
        val threadHandler = DefaultThreadHandler(threadName)
        if (!threadHandler.isActive) {
            threadHandler.start()
        }
        return threadHandler
    }

}