package com.tonyodev.dispatchandroid.utils

import android.app.Activity
import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.queuecontroller.CancelType
import com.tonyodev.dispatchandroid.queueController.ActivityDispatchQueueController

/**
 * Set's this dispatch queue to be managed by an Activity. The activity is wrapped in an instance
 * of ActivityDispatchQueueController. ActivityDispatchQueueController is controlled by the activity's lifecycle.
 * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
 * @param activity the activity that will manage the dispatch queue. The cancel type is Destroyed. Cancellation
 * occurs when the activity's onDestroy method is called.
 * @return dispatch queue.
 * */
fun<R> DispatchQueue<R>.managedBy(activity: Activity): DispatchQueue<R> {
    return managedBy(activity, CancelType.DESTROYED)
}

/**
 * Set's this dispatch queue to be managed by an Activity. The activity is wrapped in an instance
 * of ActivityDispatchQueueController. ActivityDispatchQueueController is controlled by the activity's lifecycle.
 * Managed dispatch queues can be cancelled by the DispatchQueueController if the dispatch queue is not already cancelled.
 * @param activity the activity that will manage the dispatch queue.
 * @param cancelType the cancel type
 * @return dispatch queue.
 * */
fun<R> DispatchQueue<R>.managedBy(activity: Activity, cancelType: CancelType): DispatchQueue<R> {
    val oldDispatchQueueController = controller
    oldDispatchQueueController?.unmanage(this)
    val dispatchQueueController = ActivityDispatchQueueController.getInstance(activity)
    managedBy(dispatchQueueController, cancelType)
    return this
}