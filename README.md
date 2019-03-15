[ ![Download](https://api.bintray.com/packages/tonyofrancis/maven/dispatch/images/download.svg?version=1.0.1) ](https://bintray.com/tonyofrancis/maven/dispatch/1.0.1/link)

Overview
--------

Dispatch is a simple and flexible work scheduler that schedulers work on a background or UI thread in the form of dispatch objects using android.os.Handler.

To use dispatch add the following to your app's build.gradle file
```java
implementation "com.tonyodev.dispatch:dispatch:1.0.1"
```

To use with Retrofit add
```java
    implementation "com.tonyodev.dispatch:dispatch-retrofit2-adapter:1.0.1"
```

Example:
```java
class MainActivity : AppCompatActivity() {

    private lateinit var retrofit: Retrofit
    private lateinit var service: TestService
    private val dispatchController = DispatchController.create()

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
            .baseUrl("http://someurl.com")
            .build()
        service = retrofit.create(TestService::class.java)
        runTestService()
        runTestTimer()
        runTestInterval()
    }

    private fun runTestService() {
        service.getSampleJson()
            .managedBy(dispatchController)
            .doWork { data ->
                Log.d("test", "data size is:${data.size}")
            }
            .run()

        Dispatcher.createDispatch(null)
            .managedBy(dispatchController)
            .doWork {
                service.getSampleJson().getResults()
            }
            .postMain {
                Log.d("test", "network result first item:${it.first()}")
            }
            .run()
    }

    private fun runTestTimer() {
        Dispatcher.createTimerDispatch(5000)
            .managedBy(dispatchController)
            .postMain {
                Log.d("test","Test timer after 5000 millis")
            }
            .run()
    }

    private fun runTestInterval() {
        Dispatcher.createIntervalDispatch(10000)
            .managedBy(dispatchController)
            .doWork {
                Log.d("test","Test interval every 10 seconds")
            }
            .doWork(2000) {
                Log.d("test","interval break")
                "hello world"
            }
            .postMain {
                Log.d("test","main thread break: $it")
            }
            .run()
    }

    override fun onDestroy() {
        super.onDestroy()
        dispatchController.cancelAllDispatch()
    }

}
```


Contribute
----------

Dispatch can only get better if you make code contributions. Found a bug? Report it.
Have a feature idea you'd love to see in Dispatch? Contribute to the project!


License
-------

```
Copyright (C) 2017 Tonyo Francis.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
