package com.tonyodev.dispatch.utils

import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatch.internals.DispatchQueueInfo
import com.tonyodev.dispatch.thread.ThreadHandler
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
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

internal fun throwIllegalStateExceptionIfCancelled(dispatchQueueInfo: DispatchQueueInfo) {
    if (dispatchQueueInfo.isCancelled) {
        throw IllegalStateException("Dispatch Queue with id ${dispatchQueueInfo.queueId} has already been cancelled. Cannot perform new operations.")
    }
}

internal fun startThreadHandlerIfNotActive(threadHandler: ThreadHandler) {
    if (!threadHandler.isActive) {
        threadHandler.start()
    }
}

internal fun forceLoadAndroidClassesIfAvailable() {
    if (LOAD_ANDROID_CLASSES) {
        try {
            val clazz = Class.forName("com.tonyodev.dispatchandroid.AndroidFactoriesInitializer")
            val method = clazz.getMethod("init")
            method.invoke(null)
        } catch (e: Exception) {

        }
    }
}