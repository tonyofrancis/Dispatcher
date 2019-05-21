package com.tonyodev.dispatch.queuecontroller

import com.tonyodev.dispatch.DispatchQueue

/**
 * A DispatchQueueController that uses lifecycle events to manage
 * Dispatch queues. It is a class that must be extended
 * and controlled by a lifecycle aware component.
 * */
open class LifecycleDispatchQueueController: DispatchQueueController() {

    private val queueMap = mutableMapOf<Int, Pair<DispatchQueue<*>, CancelType>>()

    /**
     * Cancels all dispatch queues that are being managed by this
     * DispatchQueueController with a cancel type of CancelType.PAUSED.
     * */
    open fun cancelAllPaused() {
        synchronized(queueMap) {
            val pausedList = queueMap.asSequence()
                .filter { it.value.second == CancelType.PAUSED }
                .map { it.value.first }
                .toList()
            super.cancelDispatchQueues(pausedList)
            for (dispatchQueue in pausedList) {
                queueMap.remove(dispatchQueue.id)
            }
        }
    }

    /**
     * Cancels all dispatch queues that are being managed by this
     * DispatchQueueController with a cancel type of CancelType.STOPPED.
     * */
    open fun cancelAllStopped() {
        synchronized(queueMap) {
            val stoppedList = queueMap.asSequence()
                .filter { it.value.second == CancelType.STOPPED }
                .map { it.value.first }
                .toList()
            super.cancelDispatchQueues(stoppedList)
            for (dispatchQueue in stoppedList) {
                queueMap.remove(dispatchQueue.id)
            }
        }
    }

    /**
     * Cancels all dispatch queues that are being managed by this
     * DispatchQueueController with a cancel type of CancelType.DESTROYED.
     * */
    open fun cancelAllDestroyed() {
        synchronized(queueMap) {
            super.cancelAllDispatchQueues()
            queueMap.clear()
        }
    }

    override fun unmanage(dispatchQueue: DispatchQueue<*>) {
        synchronized(queueMap) {
            super.unmanage(dispatchQueue)
            queueMap.remove(dispatchQueue.id)
        }
    }

    override fun unmanage(dispatchQueueList: List<DispatchQueue<*>>) {
        synchronized(queueMap) {
            super.unmanage(dispatchQueueList)
            for (dispatch in dispatchQueueList) {
                queueMap.remove(dispatch.id)
            }
        }
    }

    override fun manage(dispatchQueue: DispatchQueue<*>) {
        manage(dispatchQueue, CancelType.DESTROYED)
    }

    /**
     * Set this dispatch queue controller to manage the passed in dispatch queue with
     * the passed in cancelType.
     * @param dispatchQueue the dispatchQueue who's queue will be managed.
     * @param cancelType the cancel type.
     * */
    fun manage(dispatchQueue: DispatchQueue<*>, cancelType: CancelType) {
        synchronized(queueMap) {
            super.manage(dispatchQueue)
            queueMap[dispatchQueue.id] = Pair(dispatchQueue, cancelType)
        }
    }

    override fun manage(dispatchQueueList: List<DispatchQueue<*>>) {
        synchronized(queueMap) {
            super.manage(dispatchQueueList)
            for (dispatchQueue in dispatchQueueList) {
                queueMap[dispatchQueue.id] = Pair(dispatchQueue, CancelType.DESTROYED)
            }
        }
    }

    override fun cancelAllDispatchQueues() {
        cancelAllDestroyed()
    }

    override fun cancelDispatchQueues(vararg arrayOfDispatchQueues: DispatchQueue<*>) {
        synchronized(queueMap) {
            super.cancelDispatchQueues(arrayOfDispatchQueues.toList())
            for (dispatch in arrayOfDispatchQueues) {
                queueMap.remove(dispatch.id)
            }
        }
    }

    override fun cancelDispatchQueues(dispatchQueueIds: List<Int>) {
        synchronized(queueMap) {
            super.cancelDispatchQueues(dispatchQueueIds)
            for (id in dispatchQueueIds) {
                queueMap.remove(id)
            }
        }
    }

    override fun cancelDispatchQueues(vararg arrayOfDispatchQueueId: Int) {
        cancelDispatchQueues(arrayOfDispatchQueueId.toList())
    }

    override fun cancelDispatchQueues(dispatchQueueCollection: Collection<DispatchQueue<*>>) {
        synchronized(queueMap) {
            super.cancelDispatchQueues(dispatchQueueCollection)
            for (dispatchQueue in dispatchQueueCollection) {
                queueMap.remove(dispatchQueue.id)
            }
        }
    }

    /**
     * Gets a copy of the paused managed queues.
     * @return copy of the paused manged queues in a set.
     * */
    fun getManagedPausedDispatchQueues(): Set<DispatchQueue<*>> {
        return synchronized(queueMap) {
            queueMap.asSequence()
                .filter { it.value.second == CancelType.PAUSED }
                .map { it.value.first }
                .toSet()
        }
    }

    /**
     * Gets a copy of the stopped managed queues.
     * @return copy of the stopped manged queues in a set.
     * */
    fun getManagedStoppedDispatchQueues(): Set<DispatchQueue<*>> {
        return synchronized(queueMap) {
            queueMap.asSequence()
                .filter { it.value.second == CancelType.STOPPED }
                .map { it.value.first }
                .toSet()
        }
    }

    /**
     * Gets a copy of the destroyed managed queues.
     * @return copy of the destroyed manged queues in a set.
     * */
    fun getManagedDestroyedDispatchQueues(): Set<DispatchQueue<*>> {
        return synchronized(queueMap) {
            queueMap.asSequence()
                .map { it.value.first }
                .toSet()
        }
    }

}