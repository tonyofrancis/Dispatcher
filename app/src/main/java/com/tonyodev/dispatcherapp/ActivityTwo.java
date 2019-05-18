package com.tonyodev.dispatcherapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.tonyodev.dispatch.DispatchQueue;
import com.tonyodev.dispatch.queuecontroller.CancelType;
import com.tonyodev.dispatchandroid.queueController.ActivityDispatchQueueController;
import kotlin.jvm.functions.Function1;


public class ActivityTwo extends AppCompatActivity {

    private TextView textView;
    private ActivityDispatchQueueController activityDispatchQueueController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityDispatchQueueController = ActivityDispatchQueueController.getInstance(this);
        setContentView(R.layout.activity_two);
        textView = findViewById(R.id.text_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        runIntervalTask();
    }

    private void runIntervalTask() {
        DispatchQueue.Queues.createIntervalDispatchQueue(10000)
                .managedBy(activityDispatchQueueController, CancelType.PAUSED)
                .async(aVoid -> 55)
                .async(2000, integer -> {
                    Log.d("dispatcherTest", "interval break");
                    return "hello world";
                }).post((Function1<String, Void>) s -> {
                    textView.setText(s);
                    Log.d("dispatcherTest", "main thread break:" + s);
                    return null;
                }).async((Function1<Void, Void>) aVoid -> {
                    Log.d("dispatcherTest", "void method called");
                    return null;
                }).start(dispatchQueueError -> {

                });
    }

}
