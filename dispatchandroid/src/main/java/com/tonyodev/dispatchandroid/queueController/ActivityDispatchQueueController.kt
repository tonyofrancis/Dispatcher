package com.tonyodev.dispatchandroid.queueController

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.tonyodev.dispatch.queuecontroller.LifecycleDispatchQueueController

/**
 * A DispatchQueueController that uses an activity's lifecycle to manage
 * Dispatch queues. ActivityDispatchQueueController automatically cancels and unmanages
 * any dispatch queue that is not cancelled when the activity is destroyed.
 * */
open class ActivityDispatchQueueController(protected val activity: Activity): LifecycleDispatchQueueController() {

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
                cancelAllDestroyed()
                release()
            }
        }

        override fun onActivityResumed(activity: Activity?) {}

        override fun onActivityStarted(activity: Activity?) {}

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

    }

    init {
        activity.application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
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
           return synchronized(map) {
               val controller = map[activity] ?: ActivityDispatchQueueController(
                   activity
               )
               map[activity] = controller
               controller
           }
        }

    }

}