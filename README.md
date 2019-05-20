[ ![Download](https://api.bintray.com/packages/tonyofrancis/maven/dispatch/images/download.svg?version=1.4.0) ](https://bintray.com/tonyofrancis/maven/dispatch/1.4.0/link)
# DispatchQueue: A simple work scheduler for Java, Kotlin and Android

DispatchQueue is a simple and flexible work scheduler that schedulers work on a background or main thread in the form of a dispatch queue.

```java
DispatchQueue.background
    .async {
        //do background work here
        val sb = StringBuilder()
        for (i in 0..100) {
            sb.append(i)
              .append(" ")
        }
        sb.toString()
    }
    .post { data ->
        //do ui work here
        println(data)
    }
    .start()
```
DispatchQueue makes it very clear which thread your code is running on. An Async block will always run on a background thread. A post block will always run on the ui thread. Like what you see? Read on!

One of the many problems with offloading work to a background thread in Java and Android programming, is knowing the right time to cancel the work when it is no longer needed. DispatchQueue makes it very easy to cancel a dispatch queue. Simply call the `cancel()` method on the queue. If that is not good enough, allow your component's lifecycle to manage this for you.
Android Activity Example:
```java
class SimpleActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DispatchQueue.io
            .managedBy(this)
            .async {
                //do work in background
            }
            .post {
                //handle results on ui
            }
            .start()
    }

}
```
In the above example, the queue is managed by the Activity’s life cycle, and it will be cancelled when the Activity is destroyed. What if you want to control the cancellation of the queue when the Activity pauses or stops? Sure you can! Like so:
```java
class SimpleActivity: AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        DispatchQueue.io
            .managedBy(this, CancelType.PAUSED)
            .async {
                //do work in background
            }
            .post {
                //handle results on ui
            }
            .start()
    }

}
```
In this example, the queue is canceled when the Activity’s onPause method is called. There is no need to store the queue in a variable and cancel in manually in a callback method. You can if you want. The choice is yours.

Dispatch uses a `DispatchQueueController` to manage when a queue is canceled. There are many variations of the DispatchQueueController : `LifecycleDispatchQueueController` and `ActivityDispatchQueueController`. You can extend any of those classes to create your own queue controllers and set them on a queue.
```java
class SimpleActivity: AppCompatActivity() {

    private val dispatchQueueController = DispatchQueueController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DispatchQueue.background
            .managedBy(dispatchQueueController)
            .async {
                //do work in background
            }
            .post {
                //handle results on ui
            }
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        dispatchQueueController.cancelAllDispatchQueues()
    }

}
```
Managing queues could not be easier.
### Queue Types

DispatchQueue comes with many pre-exiting queues:
```java
DispatchQueue.background

DispatchQueue.io

DispatchQueue.test - // Used specifically for testing

```
These queues are generated only when you need/access them. You can also create you own dispatch queues via the many static create methods on the DispatchQueue.Queue class. If using Kotlin, you can call `DispatchQueue.create` directly.
```java
DispatchQueue.createDispatchQueue()

DispatchQueue.createDispatchQueue(ThreadType.NEW)

DispatchQueue.createIntervalDispatchQueue(delayInMillis = 1_000)

DispatchQueue.createTimerDispatchQueue(delayInMillis = 10_000)
```
These are just some of the queues you can create.
### Network Queues with Retrofit

We all know and love the [Retrofit](https://square.github.io/retrofit/) library created by the wonderful people at [Square](https://squareup.com/us/en). DispatchQueue works seamlessly with your Retrofit code! Let’s walkthrough a simple service example.

*TestJsonData.kt*
```java
class TestJsonData {

    var id: Int = 0

    var nm: String = ""

    var cty: String = ""

    override fun toString(): String {
        return "TestJsonData(id=$id, nm='$nm', cty='$cty')"
    }

}
```
*TestService.kt*
```java
interface TestService {

    @GET("/api/data?list=englishmonarchs&format=json")
    fun getSampleJson(): DispatchQueue<List<TestJsonData>>

}
```
*SimpleActivity.kt*
```java
class SimpleActivity: AppCompatActivity() {

    private lateinit var retrofit: Retrofit
    private lateinit var service: TestService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dispatchQueueCallAdapterFactory = DispatchQueueCallAdapterFactory.create()

        retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(dispatchQueueCallAdapterFactory)
            .baseUrl("http://mysafeinfo.com")
            .build()
        service = retrofit.create(TestService::class.java)

        runTestService()
    }

    private fun runTestService() {
        service.getSampleJson()
            .managedBy(this)
            .post { data ->
                for (testJsonData in data) {
                    println(testJsonData)
                }
            }
            .start()
    }

}
```
See how super easy it is to integrate DispatchQueue with Retrofit? All you need is a `DispatchQueueCallAdapterFactory` instance, and set your Service methods to return the data wrapped in a DispatchQueue object.
### Zipping Queues

There will be times when you would like to combine the results of two or more queues. Call the zip methods to do this.
```java
class SimpleActivity: AppCompatActivity() {

    private val lifecycleDispatchQueueController = LifecycleDispatchQueueController()

    override fun onResume() {
        super.onResume()
        DispatchQueue.background
            .managedBy(lifecycleDispatchQueueController, CancelType.PAUSED)
            .async {
                mapOf(0 to "cat", 1 to "bat")
            }
            .zip(getDataDispatchQueue()) // combine two queue results
            .async { results ->
                for ((key, value) in results.first) {
                    println("$key:$value")
                }
                for (string in results.second) {
                    println(string)
                }
            }
            .start()
    }

    private fun getDataDispatchQueue(): DispatchQueue<List<String>> {
        return Dispatcher.background
            .async {
                listOf("hat", "sat")
            }
    }

    override fun onPause() {
        super.onPause()
        lifecycleDispatchQueueController.cancelAllPaused()
    }

}
```
### Handling Errors

DispatchQueue allows you to handles errors in many ways. One way is setting an error handler for the queue by passing it to the start method.
```java
DispatchQueue.createDispatchQueue()
    .async {
        //do work
        val number = 66
        throw Exception("silly exception")
        number
    }
    .post { number ->
        println("number is $number")
    }
    .start(DispatchQueueErrorCallback { error ->
        //handle queue error here.
        Log.e("errorTest",
            "queue with id ${error.dispatchQueue.id} throw error:", error.throwable)
    })
```
**Note** in this example the post block is never executed. It can’t because the async block was not able to provide it with the data need. So the queue calls the error handler and then cancels.

Another way to handle errors more elegantly, is to provide a `doOnError` block that can return a default or valid data for the preceding async or post block and allowing the execution of the following async or post blocks.
```java
DispatchQueue.createDispatchQueue()
    .async {
        //do work
        val number = 66
        throw Exception("silly exception")
        number
    }
    .doOnError { throwable ->
        if (throwable.message == "silly exception") {
            100
        } else {
            0
        }
    }
    .post { number ->
        println("number is $number")
    }
    .start()
```
In the above example, the `doOnError` block handles the exception for the preceding async block allowing the post block to be called and the queue terminates normally. It is always good practice to provide a queue with an error handler via the start method to handle errors that were not caught in the `doOnError` blocks.

If an error handler is not provided for the queue, the exception will be thrown causing the application to crash. To prevent this, the library allows you to provide a global error handler that will catch all exceptions thrown when using any dispatch queue. It is best practice to handle errors locally close to the location where they originated. Set the global error handler like this:
```java
 DispatchQueue.globalSettings.dispatchQueueErrorCallback = DispatchQueueErrorCallback { error ->
            //handle errors
}
```

### Debugging

Figuring out where an error occurred is not always easy. This is one of the areas DispatchQueue shines. DispatchQueue allows you to set the block label for each post and async block via the `setBlockLabel(label)` method. With this information, you can check the dispatch queue id and know exactly where the issue occurred. The following example shows how this is done.
```java
class SimpleActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DispatchQueue.background
            .managedBy(this)
            .async {
                //do work
                val number = 66
                throw Exception("silly exception")
                number
            }
            .setBlockLabel("numberAsync")
            .post { number ->
                println("number is $number")
            }
            .setBlockLabel("printAsync")
            .start(DispatchQueueErrorCallback { error ->
                  if (error.blockLabel == "numberAsync") {
                    //error occurred in first async block.
                 }
            })
    }

}
```
You can also enable logging in the library. This will warm you when you forget to manage a queue with a `DispatchQueueController`.
```java
DispatchQueue.globalSettings.enableLogWarnings = true
```
### Dispatch Queue Observers

Hey kids! Here! Have more ice-cream!

DispatchQueue does not stop at solving threading problems. Introducing `DispatchQueueObserver`! Every now and then you would like a callback from the dispatch queue object that returns a result without it being directly available. That’s where DispatchQueueObservers come into play. You can attach a `DispatchQueueObserver` to a dispatch queue object and get a callback when the return value is available.
```java
class SimpleActivity: AppCompatActivity() {

    private var n = 0

    private val dispatchQueueObserver = object: DispatchQueueObserver<Int> {
        override fun onChanged(data: Int) {
            print("Factorial of $n is: $data")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        n = 16

        DispatchQueue.background
            .managedBy(this)
            .async {
                factorial(n)
            }
            .addObserver(dispatchQueueObserver)
            .start()
    }

    private fun factorial(n: Int): Int {
        if (n == 0) return 1
        return n * factorial(n - 1)
    }

}
```
### Delays

Both the async and post methods allow you to specify a delay in milliseconds before the block is executed.
```java
DispatchQueue.background
    .post(5000) {
        //will be called after a 5 second delay
    }.start()
```
### Thread Handlers

Sometimes you would like to dictate the thread that DispatchQueue uses to process blocks in the background. The library allows you to do so by providing your own Thread Handlers. Simply extend the ThreadHandler class, or use the DefaultThreadHandler and AndroidThreadHandler classes.
```java
val threadHandler = DefaultThreadHandler("MyThreadHandler")
val androidThreadHandler = AndroidThreadHandler("MyAndroidThreadHandler")

DispatchQueue.createDispatchQueue(threadHandler)
    .async {
        //do work on my own thread
    }
    .start()
```
### Understanding how DispatchQueue Works

Now that you have seen many of library’s features, it is time to give you a short summary about how it really works. You can skip this section and head to the following section on how to add DispatchQueue to your Android and Java projects.

![alt text](https://cdn-images-1.medium.com/max/800/1*C8xQEB-0U35MbDQ1W6Pq5g.png "Simple Dispatch Diagram")


The above is a simple diagram on how a dispatch queue works. When you create a queue it returns a dispatch queue object. The dispatch queue object is responsible for the thread it performs its work on, performing the work, and returning the results. The async, post and map blocks each create a new dispatch queue object and adds it to the dispatch queue when called. Hence the reason you are able to chain dispatch queue objects and pass along their results to the next dispatch queue block in the queue. The overhead for creating dispatch objects are minimal. Each dispatch queue object can have its own `doOnError` handler block and manage its own DispatchQueueObservers.

**Note**: Once a Dispatch queue has completed its work, it is then cancelled and cannot be reused.
### Using Dispatch

To use the DispatchQueue library in your project, add the following code to your project’s build.gradle file.
```java
implementation "com.tonyodev.dispatch:dispatch:1.4.0"
```
For Android also add:
```java
implementation "com.tonyodev.dispatch:dispatch-android:1.4.0"
```
To use Dispatch with Retrofit, add:
```java
implementation "com.tonyodev.dispatch:dispatch-retrofit2-adapter:1.4.0"
```

Contribute
----------

DispatchQueue can only get better if you make code contributions. Found a bug? Report it.
Have a feature idea you'd love to see in DispatchQueue? Contribute to the project!


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
