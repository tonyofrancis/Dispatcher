package com.tonyodev.dispatch

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * A DispatchQueueController that uses an activity's lifecycle to manage
 * Dispatch queues. ActivityDispatchQueueController automatically cancels and unmanages
 * any dispatch queue that is not cancelled when the activity is destroyed.
 * */
open class ActivityDispatchQueueController(private val activity: Activity): DispatchQueueController() {

    private val activityLifecycleCallbacks = object: Application.ActivityLifecycleCallbacks {

        override fun onActivityPaused(activity: Activity?) {
            if (this@ActivityDispatchQueueController.activity == activity) {
                cancelAllPaused()
            }
        }

        override fun onActivityStopped(activity: Activity?) {
            if (this@ActivityDispatchQueueController.activity == activity) {
                cancelAllStopped()
            }
        }

        override fun onActivityDestroyed(activity: Activity?) {
            if (this@ActivityDispatchQueueController.activity == activity) {
                cancelAll()
                release()
            }
        }

        override fun onActivityResumed(activity: Activity?) {}

        override fun onActivityStarted(activity: Activity?) {}

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

    }

    private val pausedQueueSet = mutableSetOf<Dispatch<*>>()

    private val stoppedQueueSet = mutableSetOf<Dispatch<*>>()

    private val destroyQueueSet = mutableSetOf<Dispatch<*>>()

    init {
        activity.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    fun cancelAllPaused() {
        super.cancelDispatch(pausedQueueSet)
        val iterator = pausedQueueSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    fun cancelAllStopped() {
        super.cancelDispatch(stoppedQueueSet)
        val iterator = stoppedQueueSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    fun cancelAll() {
        super.cancelAllDispatch()
        var iterator = pausedQueueSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
        iterator = stoppedQueueSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
        iterator = destroyQueueSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    private fun release() {
        activity.application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        val iterator = map.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().key == activity) {
                iterator.remove()
                break
            }
        }
    }

    override fun unmanage(dispatch: Dispatch<*>) {
        super.unmanage(dispatch)
        var iterator = pausedQueueSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == dispatch.rootDispatch) {
                iterator.remove()
                return
            }
        }
        iterator = stoppedQueueSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == dispatch.rootDispatch) {
                iterator.remove()
                return
            }
        }
        iterator = destroyQueueSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == dispatch.rootDispatch) {
                iterator.remove()
                break
            }
        }
    }

    override fun unmanage(dispatchList: List<Dispatch<*>>) {
        for (dispatch in dispatchList) {
            unmanage(dispatch)
        }
    }

    override fun manage(dispatch: Dispatch<*>) {
        manage(dispatch, CancelType.DESTROYED)
    }

    fun manage(dispatch: Dispatch<*>, cancelType: CancelType) {
        super.manage(dispatch)
        when(cancelType) {
            CancelType.PAUSED -> pausedQueueSet.add(dispatch.rootDispatch)
            CancelType.STOPPED -> stoppedQueueSet.add(dispatch.rootDispatch)
            CancelType.DESTROYED -> destroyQueueSet.add(dispatch.rootDispatch)
        }
    }

    override fun manage(dispatchList: List<Dispatch<*>>) {
        for (dispatch in dispatchList) {
            manage(dispatch, CancelType.DESTROYED)
        }
    }

    override fun cancelAllDispatch() {
        cancelAll()
    }

    override fun cancelDispatch(vararg arrayOfDispatch: Dispatch<*>) {
        super.cancelDispatch(*arrayOfDispatch)
        for (dispatch in arrayOfDispatch) {
            remove(dispatch.queueId)
        }
    }

    override fun cancelDispatch(dispatchQueueIds: List<Int>) {
        super.cancelDispatch(dispatchQueueIds)
        for (queueId in dispatchQueueIds) {
            remove(queueId)
        }
    }

    override fun cancelDispatch(vararg arrayOfDispatchQueueId: Int) {
        super.cancelDispatch(*arrayOfDispatchQueueId)
        for (queueId in arrayOfDispatchQueueId) {
            remove(queueId)
        }
    }

    override fun cancelDispatch(dispatchCollection: Collection<Dispatch<*>>) {
        for (dispatch in dispatchCollection) {
            super.cancelDispatch(dispatch)
            remove(dispatch.queueId)
        }
    }

    private fun remove(queueId: Int) {
        var iterator = pausedQueueSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().queueId == queueId) {
                iterator.remove()
                return
            }
        }
        iterator = stoppedQueueSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().queueId == queueId) {
                iterator.remove()
                return
            }
        }
        iterator = destroyQueueSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().queueId == queueId) {
                iterator.remove()
                break
            }
        }
    }

    companion object {

        @JvmStatic
        private val map = mutableMapOf<Activity, ActivityDispatchQueueController>()

        /**
         * Gets the ActivityDispatchQueueController instance for the passed in activity.
         * @param activity the activity.
         * @return return instance.
         * */
        @JvmStatic
        fun getInstance(activity: Activity): ActivityDispatchQueueController {
            val controller = map[activity] ?: ActivityDispatchQueueController(activity)
            map[activity] = controller
            return controller
        }

    }

}