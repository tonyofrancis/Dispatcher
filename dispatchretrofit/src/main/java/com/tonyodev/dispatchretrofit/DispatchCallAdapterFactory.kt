package com.tonyodev.dispatchretrofit

import android.os.Handler
import android.os.Looper
import com.tonyodev.dispatch.Dispatch
import com.tonyodev.dispatch.Dispatcher
import com.tonyodev.dispatch.ThreadType
import okhttp3.Request
import retrofit2.*
import java.lang.IllegalArgumentException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Dispatch Call Adapter Factory for Retrofit.
 * */
class DispatchCallAdapterFactory constructor(
    /** Optional handler used by the Dispatch objects created by this Factory.*/
    private val handler: Handler? = null,
    /** Optional error handler for network requests made by a DispatchCallAdapter instance.
     * Only called in an error occurred. Note: The errors will still be thrown inside the Dispatch.
     * This callback only allows for observing at a global level. Called on a background thread. */
    private val errorHandler: ((HttpException, Request) -> Unit)?): CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val clazz = getRawType(returnType)
        if (clazz == Dispatch::class.java) {
            if (returnType !is ParameterizedType) {
                throw IllegalArgumentException("Dispatch return type must be parameterized as Dispatch<Foo>")
            }
            val responseType = getParameterUpperBound(0, returnType)
            return DispatchCallAdapter<Any>(responseType, handler, errorHandler)
        }
        return null
    }

    companion object {

        /**
         * Creates an instance of DispatchCallAdapterFactory
         * @param handler Optional handler used to start this dispatch object.
         * @param errorHandler Optional global error handler. Called on a background thread.
         * @throws IllegalArgumentException is the passed in handler uses the main thread to do background work.
         * @return new instance of DispatchCallAdapterFactory.
         * */
        @JvmStatic
        @JvmOverloads
        fun create(handler: Handler? = null, errorHandler: ((HttpException, Request) -> Unit)? = null): DispatchCallAdapterFactory {
            if (handler?.looper?.thread?.name == Looper.getMainLooper().thread.name) {
                throw IllegalArgumentException("DispatchCallAdapterFactory: Handler cannot use the main thread for network operations.")
            }
            return DispatchCallAdapterFactory(handler, errorHandler)
        }

    }

    class DispatchCallAdapter<R>(private val responseType: Type,
                                 private val handler: Handler?,
                                 private val errorHandler: ((HttpException, Request) -> Unit)?): CallAdapter<R, Dispatch<*>> {

        override fun adapt(call: Call<R>): Dispatch<*> {
            return if (handler == null) {
                Dispatcher.createDispatchQueue(ThreadType.NETWORK)
            } else {
                Dispatcher.createDispatchQueue(handler)
            }.async {
                val callClone = call.clone()
                val response = callClone.execute()
                val data: Any? = if (response.isSuccessful) {
                    response.body()
                } else {
                    HttpException(response)
                }
                callClone.cancel()
                if (data is HttpException) {
                    try {
                        errorHandler?.invoke(data, callClone.request())
                    }catch ( e: Exception) { }
                    throw data
                }
                data
            }
        }

        override fun responseType(): Type {
            return responseType
        }

    }

}