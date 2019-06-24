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

internal fun throwIfUsesMainThreadForBackgroundWork(threadHandler: ThreadHandler?) {
    if (threadHandler != null && threadHandler.threadName == Threader.getHandlerThreadInfo(ThreadType.MAIN).threadName) {
        throw IllegalArgumentException("DispatchQueue cannot use the main threadHandler to perform background work." +
                "Pass in a threadHandler that uses a different thread.")
    }
}

internal fun throwIfUsesMainThreadForBackgroundWork(threadType: ThreadType) {
    if (threadType == ThreadType.MAIN) {
        throw IllegalArgumentException("DispatchQueue cannot use the main threadHandler to perform background work." +
                "Pass in a threadHandler that uses a different thread.")
    }
}

internal fun throwIllegalStateExceptionIfCancelled(dispatchQueueInfo: DispatchQueueInfo) {
    if (dispatchQueueInfo.isCancelled) {
        throw IllegalStateException("DispatchQueue with id: ${dispatchQueueInfo.queueId} has already been cancelled. Cannot perform new operations.")
    }
}

internal fun throwIllegalStateExceptionIfStarted(dispatchQueueInfo: DispatchQueueInfo) {
    if (dispatchQueueInfo.isStarted) {
        throw IllegalStateException("DispatchQueue with id: ${dispatchQueueInfo.queueId} has already been started. Cannot add new operations.")
    }
}

internal fun throwIllegalArgumentExceptionIfListEmpty(list: List<Any?>) {
    if (list.isEmpty()) {
        throw IllegalArgumentException("Cannot pass in an empty list.")
    }
}

internal fun startThreadHandlerIfNotActive(threadHandler: ThreadHandler) {
    if (!threadHandler.isActive) {
        threadHandler.start()
    }
}