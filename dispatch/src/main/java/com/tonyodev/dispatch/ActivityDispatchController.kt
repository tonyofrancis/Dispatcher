package com.tonyodev.dispatch

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * A Dispatch Controller that used an activity's lifecycle to manage
 * Dispatch objects.
 * */
open class ActivityDispatchController(private val activity: Activity): DispatchController() {

    private val activityLifecycleCallbacks = object: Application.ActivityLifecycleCallbacks {

        override fun onActivityPaused(activity: Activity?) {
            if (this@ActivityDispatchController.activity == activity) {
                cancelAllPaused()
            }
        }

        override fun onActivityStopped(activity: Activity?) {
            if (this@ActivityDispatchController.activity == activity) {
                cancelAllStopped()
            }
        }

        override fun onActivityDestroyed(activity: Activity?) {
            if (this@ActivityDispatchController.activity == activity) {
                cancelAll()
                release()
            }
        }

        override fun onActivityResumed(activity: Activity?) {}

        override fun onActivityStarted(activity: Activity?) {}

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

    }

    private val pausedSet = mutableSetOf<Dispatch<*>>()

    private val stoppedSet = mutableSetOf<Dispatch<*>>()

    private val destroySet = mutableSetOf<Dispatch<*>>()

    init {
        activity.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    fun cancelAllPaused() {
        super.cancelDispatch(*pausedSet.toTypedArray())
        val iterator = pausedSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    fun cancelAllStopped() {
        super.cancelDispatch(*stoppedSet.toTypedArray())
        val iterator = stoppedSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    fun cancelAll() {
        super.cancelAllDispatch()
        var iterator = pausedSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
        iterator = stoppedSet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
        iterator = destroySet.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    private fun release() {
        synchronized(map) {
            activity.application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
            map.remove(activity)
        }
    }

    override fun unmanage(dispatch: Dispatch<*>) {
        super.unmanage(dispatch)
        var iterator = pausedSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == dispatch.rootDispatch) {
                iterator.remove()
                break
            }
        }
        iterator = stoppedSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == dispatch.rootDispatch) {
                iterator.remove()
                break
            }
        }
        iterator = destroySet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() == dispatch.rootDispatch) {
                iterator.remove()
                break
            }
        }
    }

    override fun manage(dispatch: Dispatch<*>) {
        manage(dispatch, CancelType.DESTROYED)
    }

    fun manage(dispatch: Dispatch<*>, cancelType: CancelType) {
        super.manage(dispatch)
        when(cancelType) {
            CancelType.PAUSED -> pausedSet.add(dispatch.rootDispatch)
            CancelType.STOPPED -> stoppedSet.add(dispatch.rootDispatch)
            CancelType.DESTROYED -> destroySet.add(dispatch.rootDispatch)
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

    private fun remove(queueId: Int) {
        var iterator = pausedSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().queueId == queueId) {
                iterator.remove()
                break
            }
        }
        iterator = stoppedSet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().queueId == queueId) {
                iterator.remove()
                break
            }
        }
        iterator = destroySet.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().queueId == queueId) {
                iterator.remove()
                break
            }
        }
    }

    companion object {

        @JvmStatic
        private val map = mutableMapOf<Activity, ActivityDispatchController>()

        @JvmStatic
        /**
         * Gets the ActivityDispatchController instance for the passed in activity.
         * @param activity the activity.
         * @return return instance.
         * */
        fun getInstance(activity: Activity): ActivityDispatchController {
            return synchronized(map) {
                val controller = map[activity] ?: ActivityDispatchController(activity)
                map[activity] = controller
                controller
            }
        }

    }

}