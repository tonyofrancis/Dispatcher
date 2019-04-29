package com.tonyodev.dispatch.utils

import android.os.Handler
import android.os.Looper
import com.tonyodev.dispatch.ThreadType
import java.lang.IllegalArgumentException
import java.util.*

internal fun getNewQueueId(): Int {
    return UUID.randomUUID().hashCode()
}

internal fun getNewDispatchId(): String {
    return UUID.randomUUID().toString()
}

internal fun throwIfUsesMainThreadForBackgroundWork(handler: Handler?) {
    if (handler != null && handler.looper.thread.name == Looper.getMainLooper().thread.name) {
        throw IllegalArgumentException("Dispatch handler cannot use the main thread to perform background work." +
                "Pass in a handler that uses a different thread.")
    }
}

internal fun throwIfUsesMainThreadForBackgroundWork(threadType: ThreadType) {
    if (threadType == ThreadType.MAIN) {
        throw IllegalArgumentException("Dispatch handler cannot use the main thread to perform background work." +
                "Pass in a handler that uses a different thread.")
    }
}