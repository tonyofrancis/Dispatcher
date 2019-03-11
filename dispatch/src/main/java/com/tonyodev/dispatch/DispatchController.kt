package com.tonyodev.dispatch

/**
 * A DispatchController manages dispatch objects and is
 * responsible for cancelling dispatch objects at the
 * appropriate time.
 * */
open class DispatchController {

    private val dispatchSet = mutableSetOf<Dispatch<*>>()

    /**
     * Set this dispatch controller to manage the passed in dispatch.
     * @param dispatch the dispatch to manage.
     * */
    open fun manage(dispatch: Dispatch<*>) {
        dispatchSet.add(dispatch.rootDispatch)
    }

    /**
     * Stop managing the passed in dispatch.
     * @param dispatch the dispatch to unmanage.
     * */
    open fun unmanage(dispatch: Dispatch<*>) {
        val iterator = dispatchSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == dispatch.rootDispatch) {
                iterator.remove()
            }
        }
    }

    /**
     * Cancels all the dispatch objects that are being managed by this
     * instance of DispatchController. All the dispatch objects will then be unmanaged.
     * */
    open fun cancelAllDispatch() {
        val iterator = dispatchSet.iterator()
        var dispatch: Dispatch<*>
        while (iterator.hasNext()) {
            dispatch = iterator.next()
            iterator.remove()
            dispatch.cancel()
        }
    }

    /**
     * Cancels all dispatch objects being managed by this
     * instance of DispatchController if it is in the passed in array.
     * The dispatch objects will no longer be managed.
     * @param arrayOfDispatch dispatch objects.
     * */
    open fun cancelDispatch(vararg arrayOfDispatch: Dispatch<*>) {
        val dispatchList = arrayOfDispatch.map { it.rootDispatch }
        val iterator = dispatchSet.iterator()
        var dispatch: Dispatch<*>
        while (iterator.hasNext()) {
            dispatch = iterator.next()
            if (dispatchList.contains(dispatch)) {
                iterator.remove()
                dispatch.cancel()
            }
        }
    }

    /**
     * Cancels dispatch objects being managed by this
     * instance of DispatchController if it's queue id is in the passed in list.
     * The dispatch objects will no longer be managed.
     * @param dispatchQueueIds the dispatch queue ids.
     * */
    open fun cancelDispatch(dispatchQueueIds: List<Int>) {
        val iterator = dispatchSet.iterator()
        var dispatch: Dispatch<*>
        while (iterator.hasNext()) {
            dispatch = iterator.next()
            if (dispatchQueueIds.contains(dispatch.queueId)) {
                iterator.remove()
                dispatch.cancel()
            }
        }
    }

    /**
     * Cancels dispatch objects being managed by this
     * instance of DispatchController if it's queue id is in the passed in array.
     * The dispatch objects will no longer be managed.
     * @param arrayOfDispatchQueueId the dispatch objects queue ids.
     * */
    open fun cancelDispatch(vararg arrayOfDispatchQueueId: Int) {
        cancelDispatch(arrayOfDispatchQueueId.toList())
    }

    companion object {

        @JvmStatic
        /**
         * Creates a new instance of DispatchController
         * */
        fun create(): DispatchController {
            return DispatchController()
        }

    }

}