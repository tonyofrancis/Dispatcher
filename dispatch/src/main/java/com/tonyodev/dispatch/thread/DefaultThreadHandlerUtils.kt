package com.tonyodev.dispatch.thread

fun findQueueMinIndex(queue: List<ThreadHandlerQueueItem>, minQueueIndexPair: DefaultThreadHandler.MinQueueIndexPair): DefaultThreadHandler.MinQueueIndexPair {
    return synchronized(queue) {
        minQueueIndexPair.index = -1
        minQueueIndexPair.waitTime = -1
        var minDelay = -1L
        var counter = 0
        var queueItem: ThreadHandlerQueueItem
        while (counter < queue.size) {
            queueItem = queue[counter]
            if (minDelay == -1L || queueItem.delay < minDelay) {
                minQueueIndexPair.index = counter
                minDelay = queueItem.delay
                minQueueIndexPair.waitTime = queueItem.delay - queueItem.waitTime
                if (minQueueIndexPair.waitTime < 0) {
                    minQueueIndexPair.waitTime = 0
                }
            }
            counter++
        }
        minQueueIndexPair
    }
}