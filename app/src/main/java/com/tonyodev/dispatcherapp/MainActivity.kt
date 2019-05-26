package com.tonyodev.dispatcherapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.tonyodev.dispatch.DispatchQueue
import com.tonyodev.dispatch.DispatchQueueErrorCallback
import com.tonyodev.dispatch.async
import com.tonyodev.dispatch.post
import com.tonyodev.dispatchandroid.managedBy
import com.tonyodev.dispatchretrofit.DispatchQueueCallAdapterFactory
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
            .addCallAdapterFactory(DispatchQueueCallAdapterFactory.create())
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build()
        service = retrofit.create(TestService::class.java)

        runTestService()
        runTestTimer()
        runDSL()
    }

    private fun runTestService() {
        service.getSampleJson()
            .managedBy(this)
            .async { data ->
                Log.d("dispatcherTest", "data size is:${data.size}")
            }
            .start()
        //or
        DispatchQueue.background
            .managedBy(this)
            .async {
                "66"
            }
            .zip(service.getSampleJson())
            .async {
                it.second
            }
            .post {
                Log.d("dispatcherTest", "data size is:${it.size}")
            }
            .start(DispatchQueueErrorCallback {

            })
    }

    private fun runTestTimer() {
        DispatchQueue.createTimerDispatchQueue(5000)
            .managedBy(this)
            .async {
                "do work here"
            }
            .post {
                "do work here"
            }
            .async {
                "do more work here"
            }
            .post {
                Log.d("dispatcherTest","Test timer after 5000 millis. data is $it")
            }
            .start(DispatchQueueErrorCallback {

            })
    }

    private fun runDSL() {
        async {
            run {
                processServiceData()
            }
        }
    }

    private fun processServiceData() {
        val data = service.getData().execute().body()
        post {
          run {
              println("This was a fun service Fetch using DSL. ")
              data?.forEach {
                  println(it.toString())
              }
          }
        }
    }

}
