package com.tonyodev.dispatchretrofit

import android.os.Handler
import android.os.Looper
import com.tonyodev.dispatch.Dispatch
import com.tonyodev.dispatch.Dispatcher
import retrofit2.*
import java.lang.IllegalArgumentException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Dispatch Call Adapter Factory for Retrofit.
 * */
class DispatchCallAdapterFactory @JvmOverloads constructor(
    /** Optional handler used by the Dispatch objects created by this Factory.*/
    private val handler: Handler? = null): CallAdapter.Factory() {

    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        val clazz = getRawType(returnType)
        if (clazz == Dispatch::class.java) {
            if (returnType !is ParameterizedType) {
                throw IllegalArgumentException("Dispatch return type must be parameterized as Dispatch<Foo>")
            }
            val responseType = getParameterUpperBound(0, returnType)
            return DispatchCallAdapter<Any>(responseType, handler)
        }
        return null
    }

    companion object {

        /**
         * Creates an instance of DispatchCallAdapterFactory
         * @param handler Optional handler used to run this dispatch object.
         * @throws IllegalArgumentException is the passed in handler uses the main thread to do background work.
         * @return new instance of DispatchCallAdapterFactory.
         * */
        @JvmStatic
        @JvmOverloads
        fun create(handler: Handler? = null): DispatchCallAdapterFactory {
            if (handler?.looper?.thread?.name == Looper.getMainLooper().thread.name) {
                throw IllegalArgumentException("Handler cannot use the main thread for network operations.")
            }
            return DispatchCallAdapterFactory(handler)
        }

    }

    class DispatchCallAdapter<R>(private val responseType: Type,
                                 private val handler: Handler?): CallAdapter<R, Dispatch<*>> {

        override fun adapt(call: Call<R>): Dispatch<*> {
            return Dispatcher.createDispatch(handler)
                .doWork {
                    val callClone = call.clone()
                    val response = callClone.execute()
                    if (response.isSuccessful) {
                        response.body()
                    } else {
                        throw HttpException(response)
                    }
                }
        }

        override fun responseType(): Type {
            return responseType
        }

    }

}