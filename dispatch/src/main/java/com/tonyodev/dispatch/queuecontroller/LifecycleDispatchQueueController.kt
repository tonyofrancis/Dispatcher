package com.tonyodev.dispatch.queuecontroller

import com.tonyodev.dispatch.DispatchQueue

/**
 * A DispatchQueueController that uses lifecycle events to manage
 * Dispatch queues. It is a class that must be extended
 * and controlled by a lifecycle aware component.
 * */
open class LifecycleDispatchQueueController: DispatchQueueController() {

    private val pausedQueueSet = mutableSetOf<DispatchQueue<*>>()

    private val stoppedQueueSet = mutableSetOf<DispatchQueue<*>>()

    private val destroyQueueSet = mutableSetOf<DispatchQueue<*>>()

    /**
     * Cancels all dispatch queues that are being managed by this
     * DispatchQueueController with a cancel type of CancelType.PAUSED.
     * */
    open fun cancelAllPaused() {
        super.cancelDispatchQueues(pausedQueueSet)
        synchronized(pausedQueueSet) {
            val iterator = pausedQueueSet.iterator()
            while (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }

    /**
     * Cancels all dispatch queues that are being managed by this
     * DispatchQueueController with a cancel type of CancelType.STOPPED.
     * */
    open fun cancelAllStopped() {
        super.cancelDispatchQueues(stoppedQueueSet)
        synchronized(stoppedQueueSet) {
            val iterator = stoppedQueueSet.iterator()
            while (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }

    /**
     * Cancels all dispatch queues that are being managed by this
     * DispatchQueueController with a cancel type of CancelType.DESTROYED.
     * */
    open fun cancelAllDestroyed() {
        super.cancelAllDispatchQueues()
        cancelAllPaused()
        cancelAllStopped()
        synchronized(destroyQueueSet) {
            val iterator = destroyQueueSet.iterator()
            while (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }

    override fun unmanage(dispatchQueue: DispatchQueue<*>) {
        super.unmanage(dispatchQueue)
        var iterator: MutableIterator<DispatchQueue<*>>
        synchronized(pausedQueueSet) {
            iterator = pausedQueueSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() == dispatchQueue.root) {
                    iterator.remove()
                    return
                }
            }
        }
        synchronized(stoppedQueueSet) {
            iterator = stoppedQueueSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() == dispatchQueue.root) {
                    iterator.remove()
                    return
                }
            }
        }
        synchronized(destroyQueueSet) {
            iterator = destroyQueueSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next() == dispatchQueue.root) {
                    iterator.remove()
                    break
                }
            }
        }
    }

    override fun unmanage(dispatchQueueList: List<DispatchQueue<*>>) {
        for (dispatch in dispatchQueueList) {
            unmanage(dispatch)
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
        super.manage(dispatchQueue)
        when(cancelType) {
            CancelType.PAUSED -> synchronized(pausedQueueSet) { pausedQueueSet.add(dispatchQueue.root) }
            CancelType.STOPPED -> synchronized(stoppedQueueSet) { stoppedQueueSet.add(dispatchQueue.root) }
            CancelType.DESTROYED -> synchronized(destroyQueueSet) { destroyQueueSet.add(dispatchQueue.root) }
        }
    }

    override fun manage(dispatchQueueList: List<DispatchQueue<*>>) {
        synchronized(destroyQueueSet) {
            for (dispatchQueue in dispatchQueueList) {
                super.manage(dispatchQueue)
                destroyQueueSet.add(dispatchQueue.root)
            }
        }
    }

    override fun cancelAllDispatchQueues() {
        cancelAllDestroyed()
    }

    override fun cancelDispatchQueues(vararg arrayOfDispatchQueues: DispatchQueue<*>) {
        super.cancelDispatchQueues(*arrayOfDispatchQueues)
        for (dispatch in arrayOfDispatchQueues) {
            remove(dispatch.id)
        }
    }

    override fun cancelDispatchQueues(dispatchQueueIds: List<Int>) {
        super.cancelDispatchQueues(dispatchQueueIds)
        for (queueId in dispatchQueueIds) {
            remove(queueId)
        }
    }

    override fun cancelDispatchQueues(vararg arrayOfDispatchQueueId: Int) {
        super.cancelDispatchQueues(*arrayOfDispatchQueueId)
        for (queueId in arrayOfDispatchQueueId) {
            remove(queueId)
        }
    }

    override fun cancelDispatchQueues(dispatchQueueCollection: Collection<DispatchQueue<*>>) {
        for (dispatch in dispatchQueueCollection) {
            super.cancelDispatchQueues(dispatch)
            remove(dispatch.id)
        }
    }

    private fun remove(queueId: Int) {
        synchronized(pausedQueueSet) {
            val iterator = pausedQueueSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().id == queueId) {
                    iterator.remove()
                    return
                }
            }
        }
        synchronized(stoppedQueueSet) {
            val iterator = stoppedQueueSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().id == queueId) {
                    iterator.remove()
                    return
                }
            }
        }
        synchronized(destroyQueueSet) {
            val iterator = destroyQueueSet.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().id == queueId) {
                    iterator.remove()
                    break
                }
            }
        }
    }

    /**
     * Gets a copy of the paused managed queues.
     * @return copy of the paused manged queues in a set.
     * */
    fun getManagedPausedDispatchQueues(): Set<DispatchQueue<*>> {
        return synchronized(pausedQueueSet) { pausedQueueSet.toSet() }
    }

    /**
     * Gets a copy of the stopped managed queues.
     * @return copy of the stopped manged queues in a set.
     * */
    fun getManagedStoppedDispatchQueues(): Set<DispatchQueue<*>> {
        return synchronized(stoppedQueueSet) { stoppedQueueSet.toSet() }
    }

    /**
     * Gets a copy of the destroyed managed queues.
     * @return copy of the destroyed manged queues in a set.
     * */
    fun getManagedDestroyedDispatchQueues(): Set<DispatchQueue<*>> {
        return synchronized(destroyQueueSet) { destroyQueueSet.toSet() }
    }

}