package com.tonyodev.dispatch.internals

import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.queuecontroller.DispatchQueueController
import com.tonyodev.dispatch.utils.Threader
import java.util.*

internal class DispatchQueueInfo(val queueId: Int,
                                 val isIntervalDispatch: Boolean = false,
                                 val threadHandlerInfo: Threader.ThreadHandlerInfo) {
    @Volatile
    var isCancelled = false
    @Volatile
    var completedDispatchQueue = false
    lateinit var rootDispatchQueue: DispatchQueueImpl<*, *>
    val queue = LinkedList<DispatchQueueImpl<*, *>>()
    var errorHandler: ((throwable: Throwable, dispatchQueue: DispatchQueue<*>, blockLabel: String) -> Unit)? = null
    var dispatchQueueController: DispatchQueueController? = null

}