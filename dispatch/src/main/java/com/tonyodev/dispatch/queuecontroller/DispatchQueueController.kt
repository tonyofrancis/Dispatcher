package com.tonyodev.dispatch.queuecontroller

import com.tonyodev.dispatch.DispatchQueue

/**
 * A DispatchQueueController manages dispatch queues and is
 * responsible for cancelling a dispatch queue at the appropriate time.
 * */
open class DispatchQueueController {

    private val dispatchQueueMap = mutableMapOf<Int, DispatchQueue<*>>()

    /**
     * Set this dispatch queue controller to manage the passed in dispatch queue.
     * @param dispatchQueue the dispatchQueue who's queue will be managed.
     * */
    open fun manage(dispatchQueue: DispatchQueue<*>) {
        synchronized(dispatchQueueMap) {
            dispatchQueueMap[dispatchQueue.id] = dispatchQueue
        }
    }

    /**
     * Set this dispatch queue controller to manage the passed in dispatch queue.
     * @param dispatchQueueList a list of dispatch who's queue will be managed.
     * */
    open fun manage(dispatchQueueList: List<DispatchQueue<*>>) {
        synchronized(dispatchQueueMap) {
            for (dispatchQueue in dispatchQueueList) {
                dispatchQueueMap[dispatchQueue.id] = dispatchQueue
            }
        }
    }

    /**
     * Stop managing the passed in dispatch queue.
     * @param dispatchQueue the dispatch queue to unmanage.
     * */
    open fun unmanage(dispatchQueue: DispatchQueue<*>) {
        synchronized(dispatchQueueMap) {
            dispatchQueueMap.remove(dispatchQueue.id)
        }
    }

    /**
     * Stop managing the passed in list dispatch queues.
     * @param dispatchQueueList the list of dispatch queues to unmanage.
     * */
    open fun unmanage(dispatchQueueList: List<DispatchQueue<*>>) {
        synchronized(dispatchQueueMap) {
            for (dispatchQueue in dispatchQueueList) {
                dispatchQueueMap.remove(dispatchQueue.id)
            }
        }
    }

    /**
     * Cancels all the dispatch queues that are being managed by this
     * instance of DispatchQueueController. All the dispatch queues will then be unmanaged.
     * */
    open fun cancelAllDispatchQueues() {
        synchronized(dispatchQueueMap) {
            val iterator = dispatchQueueMap.iterator()
            var dispatchQueue: DispatchQueue<*>
            while (iterator.hasNext()) {
                dispatchQueue = iterator.next().value
                iterator.remove()
                dispatchQueue.cancel()
            }
        }
    }

    /**
     * Cancels all dispatch queues being managed by this
     * instance of DispatchQueueController if it is in the passed in array.
     * The dispatch queue will no longer be managed.
     * @param arrayOfDispatchQueues dispatch queue objects.
     * */
    open fun cancelDispatchQueues(vararg arrayOfDispatchQueues: DispatchQueue<*>) {
        cancelDispatchQueues(arrayOfDispatchQueues.map { it })
    }

    /**
     * Cancels all dispatch queues being managed by this
     * instance of DispatchQueueController if it is in the passed in collection.
     * The dispatch queue will no longer be managed.
     * @param dispatchQueueCollection dispatch queues.
     * */
    open fun cancelDispatchQueues(dispatchQueueCollection: Collection<DispatchQueue<*>>) {
        synchronized(dispatchQueueMap) {
            for (dispatchQueue in dispatchQueueCollection) {
                dispatchQueueMap.remove(dispatchQueue.id)?.cancel()
            }
        }
    }

    /**
     * Cancels dispatch queues being managed by this
     * instance of DispatchQueueController if it's queue id is in the passed in list.
     * The dispatch queue will no longer be managed.
     * @param dispatchQueueIds the dispatch queue ids.
     * */
    open fun cancelDispatchQueues(dispatchQueueIds: List<Int>) {
        synchronized(dispatchQueueMap) {
            for (dispatchQueueId in dispatchQueueIds) {
                dispatchQueueMap.remove(dispatchQueueId)?.cancel()
            }
        }
    }

    /**
     * Cancels the dispatch queue being managed by this
     * instance of DispatchQueueController if it's queue id is in the passed in array.
     * The dispatch queues will no longer be managed.
     * @param arrayOfDispatchQueueId the dispatch objects queue ids.
     * */
    open fun cancelDispatchQueues(vararg arrayOfDispatchQueueId: Int) {
        cancelDispatchQueues(arrayOfDispatchQueueId.toList())
    }

    /**
     * Gets a copy of the managed queues.
     * @return copy of the manged queues in a set.
     * */
    fun getManagedDispatchQueues(): Set<DispatchQueue<*>> {
        return synchronized(dispatchQueueMap) { dispatchQueueMap.values.toSet() }
    }

}