package com.tonyodev.dispatch.utils

class DefaultLogger: Logger {

    override fun print(tag: String, message: String) {
        println("$tag: $message")
    }

    override fun print(tag: String, throwable: Throwable) {
        println("$tag: ${throwable.message}: $throwable")
    }

    override fun print(tag: String, any: Any?) {
        println("$tag: $any")
    }

    override fun print(message: String) {
        println("$TAG: $message")
    }

    override fun print(throwable: Throwable) {
        println("$TAG: ${throwable.message}: $throwable")
    }

    override fun print(any: Any?) {
        println("$TAG: $any")
    }

}