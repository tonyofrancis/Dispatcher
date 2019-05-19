package com.tonyodev.dispatch.internals

import com.tonyodev.dispatch.DispatchQueueErrorCallback
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.utils.Threader
import kotlin.collections.ArrayList

internal class DispatchQueueInfo(val queueId: Int,
                                 val isIntervalDispatch: Boolean = false,
                                 val threadHandlerInfo: Threader.ThreadHandlerInfo) {
    @Volatile
    var isCancelled = false
    @Volatile
    var completedDispatchQueue = false
    @Volatile
    var isStarted = false
    lateinit var rootDispatchQueue: DispatchQueueImpl<*, *>
    val queue = ArrayList<DispatchQueueImpl<*, *>>()
    var dispatchQueueErrorCallback: DispatchQueueErrorCallback? = null
    var dispatchQueueController: DispatchQueueController? = null

}