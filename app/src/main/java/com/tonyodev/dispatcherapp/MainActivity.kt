package com.tonyodev.dispatcherapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.tonyodev.dispatch.Dispatcher
import com.tonyodev.dispatch.ThreadType
import com.tonyodev.dispatchretrofit.DispatchCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var retrofit: Retrofit
    private lateinit var service: TestService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button).setOnClickListener {
            val intent = Intent(this, ActivityTwo::class.java)
            startActivity(intent)
        }
        retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(DispatchCallAdapterFactory.create())
            .baseUrl("http://mysafeinfo.com")
            .build()
        service = retrofit.create(TestService::class.java)
        runTestService()
        runTestTimer()
    }

    private fun runTestService() {
       service.getSampleJson()
           .managedBy(this)
            .doWork { data ->
                Log.d("dispatcherTest", "data size is:${data.size}")
            }
           .run()
        //or
        Dispatcher.createTimerDispatch(2000)
            .managedBy(this)
            .doWork { 124 }
            .combine(service.getSampleJson().doOnError { emptyList() })
            .doWork {
                Log.d("dispatcherTest", "data is:(number:${it.first}, listOf: ${it.second})")
            }
            .run()
    }

    private fun runTestTimer() {
        Dispatcher.createTimerDispatch(5000)
            .managedBy(this)
            .postMain {
                Log.d("dispatcherTest","Test timer after 5000 millis. data is $it")
            }
            .run()
    }

}
