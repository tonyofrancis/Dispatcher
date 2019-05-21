package com.tonyodev.dispatch.internals

import com.tonyodev.dispatch.DispatchQueueErrorCallback
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.utils.Threader

internal class DispatchQueueInfo(val queueId: Int,
                                 val isIntervalDispatch: Boolean = false,
                                 val threadHandlerInfo: Threader.ThreadHandlerInfo) {
    @Volatile
    var isCancelled = false
    @Volatile
    var isStarted = false
    var dispatchQueueErrorCallback: DispatchQueueErrorCallback? = null
    var dispatchQueueController: DispatchQueueController? = null
    var rootDispatchQueue: DispatchQueueImpl<*, *>? = null
    var endDispatchQueue: DispatchQueueImpl<*, *>? = null

    fun canPerformOperations(): Boolean {
        return !isStarted && !isCancelled
    }

}