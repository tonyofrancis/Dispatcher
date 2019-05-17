package com.tonyodev.dispatch


/**
 * This class holds the error, dispatch queue and the label of the block in the queue that caused the error.
 * */
data class DispatchQueueError(
    /**
     * The error.
     * */
    val throwable: Throwable,

    /**
     * The dispatch queue the error occurred in.
     * */
    val dispatchQueue: DispatchQueue<*>,

    /**
     * The label of the block that caused the error in the dispatch queue.
     * */
    val blockLabel: String)