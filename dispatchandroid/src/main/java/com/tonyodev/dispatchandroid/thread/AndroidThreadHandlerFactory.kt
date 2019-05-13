package com.tonyodev.dispatchandroid.thread

import android.os.Handler
import android.os.Looper
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.TestThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandler
import com.tonyodev.dispatch.thread.ThreadHandlerFactory
import com.tonyodev.dispatch.utils.*

/**
 * The default ThreadHandler Factory used by the library.
 * */
class AndroidThreadHandlerFactory: ThreadHandlerFactory {

    @Volatile
    private var newThreadCount = 0

    override fun create(threadType: ThreadType): ThreadHandler {
        return when(threadType) {
            ThreadType.BACKGROUND -> AndroidThreadHandler(THREAD_BACKGROUND)
            ThreadType.BACKGROUND_SECONDARY -> AndroidThreadHandler(THREAD_BACKGROUND_SECONDARY)
            ThreadType.NETWORK -> AndroidThreadHandler(THREAD_NETWORK)
            ThreadType.IO -> AndroidThreadHandler(THREAD_IO)
            ThreadType.NEW -> getNewDispatchHandler()
            ThreadType.MAIN -> AndroidThreadHandler(Handler(Looper.getMainLooper()))
            ThreadType.TEST -> TestThreadHandler(THREAD_TEST)
        }
    }

    override fun create(threadName: String?): ThreadHandler {
        return getNewDispatchHandler(threadName)
    }

    private fun getNewDispatchHandler(name: String? = null): ThreadHandler {
        val threadName = if (name == null || name.isEmpty()) {
            "dispatch${++newThreadCount}"
        } else {
            name
        }
        return AndroidThreadHandler(threadName)
    }

}