package com.tonyodev.dispatch.utils

/**
 * Logger interface used by the library. Can be used to created custom loggers.
 * */
interface Logger {

    /**
     * log tag and message.
     * @param tag the tag.
     * @param message the message.
     * */
    fun print(tag: String, message: String)

    /**
     * log tag and throwable.
     * @param tag the tag.
     * @param throwable the throwable.
     * */
    fun print(tag: String, throwable: Throwable)

    /**
     * log tag and message.
     * @param tag the tag.
     * @param any the object.
     * */
    fun print(tag: String, any: Any?)

    /**
     * log message.
     * @param message the message.
     * */
    fun print(message: String)

    /**
     * log throwable.
     * @param throwable the throwable.
     * */
    fun print(throwable: Throwable)

    /**
     * log object.
     * @param any the object.
     * */
    fun print(any: Any?)

}