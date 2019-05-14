package com.tonyodev.dispatch.queuecontroller

import com.tonyodev.dispatch.DispatchQueue

/**
 * A DispatchQueueController manages dispatch queues and is
 * responsible for cancelling a dispatch queue at the appropriate time.
 * */
open class DispatchQueueController {

    private val dispatchQueueSet = mutableSetOf<DispatchQueue<*>>()

    /**
     * Set this dispatch queue controller to manage the passed in dispatch queue.
     * @param dispatchQueue the dispatchQueue who's queue will be managed.
     * */
    open fun manage(dispatchQueue: DispatchQueue<*>) {
        synchronized(dispatchQueueSet) {
            dispatchQueueSet.add(dispatchQueue.root)
        }
    }

    /**
     * Set this dispatch queue controller to manage the passed in dispatch queue.
     * @param dispatchQueueList a list of dispatch who's queue will be managed.
     * */
    open fun manage(dispatchQueueList: List<DispatchQueue<*>>) {
        for (dispatch in dispatchQueueList) {
            manage(dispatch)
        }
    }

    /**
     * Stop managing the passed in dispatch queue.
     * @param dispatchQueue the dispatch queue to unmanage.
     * */
    open fun unmanage(dispatchQueue: DispatchQueue<*>) {
        synchronized(dispatchQueueSet) {
            val iterator = dispatchQueueSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() == dispatchQueue.root) {
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Stop managing the passed in list dispatch queues.
     * @param dispatchQueueList the list of dispatch queues to unmanage.
     * */
    open fun unmanage(dispatchQueueList: List<DispatchQueue<*>>) {
        synchronized(dispatchQueueSet) {
            val iterator = dispatchQueueSet.iterator()
            var count = 0
            while (iterator.hasNext()) {
                if (dispatchQueueList.contains(iterator.next().root)) {
                    iterator.remove()
                    ++count
                    if (count == dispatchQueueList.size) {
                        break
                    }
                }
            }
        }
    }

    /**
     * Cancels all the dispatch queues that are being managed by this
     * instance of DispatchQueueController. All the dispatch queues will then be unmanaged.
     * */
    open fun cancelAllDispatchQueues() {
        synchronized(dispatchQueueSet) {
            val iterator = dispatchQueueSet.iterator()
            var dispatch: DispatchQueue<*>
            while (iterator.hasNext()) {
                dispatch = iterator.next()
                iterator.remove()
                dispatch.cancel()
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
        cancelDispatchQueues(arrayOfDispatchQueues.map { it.root })
    }

    /**
     * Cancels all dispatch queues being managed by this
     * instance of DispatchQueueController if it is in the passed in collection.
     * The dispatch queue will no longer be managed.
     * @param dispatchQueueCollection dispatch queues.
     * */
    open fun cancelDispatchQueues(dispatchQueueCollection: Collection<DispatchQueue<*>>) {
        synchronized(dispatchQueueSet) {
            val iterator = dispatchQueueSet.iterator()
            var dispatch: DispatchQueue<*>
            var count = 0
            while (iterator.hasNext()) {
                dispatch = iterator.next()
                if (dispatchQueueCollection.contains(dispatch)) {
                    iterator.remove()
                    dispatch.cancel()
                    ++count
                    if (count == dispatchQueueCollection.size) {
                        break
                    }
                }
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
        synchronized(dispatchQueueSet) {
            val iterator = dispatchQueueSet.iterator()
            var dispatch: DispatchQueue<*>
            var count = 0
            while (iterator.hasNext()) {
                dispatch = iterator.next()
                if (dispatchQueueIds.contains(dispatch.id)) {
                    iterator.remove()
                    dispatch.cancel()
                    ++count
                    if (count == dispatchQueueIds.size) {
                        break
                    }
                }
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
        return synchronized(dispatchQueueSet) { dispatchQueueSet.toSet() }
    }

}