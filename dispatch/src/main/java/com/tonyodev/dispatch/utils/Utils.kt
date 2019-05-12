package com.tonyodev.dispatch.utils

import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.thread.ThreadHandler
import java.lang.IllegalArgumentException
import java.util.*

internal fun getNewQueueId(): Int {
    return UUID.randomUUID().hashCode()
}

internal fun getNewDispatchId(): String {
    return UUID.randomUUID().toString()
}

internal fun throwIfUsesMainThreadForBackgroundWork(handler: ThreadHandler?) {
    if (handler != null && handler.threadName == Threader.getHandlerThreadInfo(ThreadType.MAIN).threadName) {
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

internal fun forceLoadAndroidClassesIfAvailable() {
    try {
        val clazz = Class.forName("com.tonyodev.dispatchandroid.AndroidFactoriesInitializer")
        val method = clazz.getMethod("init")
        method.invoke(null)
    } catch (e: Exception) {

    }
}