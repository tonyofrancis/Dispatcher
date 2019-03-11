package com.tonyodev.dispatcherapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.tonyodev.dispatch.Dispatcher;
import com.tonyodev.dispatch.DispatchController;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class ActivityTwo extends AppCompatActivity {

    private final DispatchController dispatchController = DispatchController.create();
    private TextView textView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two);
        textView = findViewById(R.id.text_view);
        runIntervalTask();
    }

    private void runIntervalTask() {
        Dispatcher.createIntervalDispatch(10000)
                .managedBy(dispatchController)
                .doWork(new Function1<Unit, Integer>() {
                    @Override
                    public Integer invoke(Unit unit) {
                        return 55;
                    }
                })
                .doWork(2000, new Function1<Integer, String>() {
            @Override
            public String invoke(Integer integer) {
                Log.d("dispatcherTest", "interval break");
                return "hello world";
            }
        }).postMain(new Function1<String, Void>() {
            @Override
            public Void invoke(String s) {
                textView.setText(s);
                Log.d("dispatcherTest", "main thread break:" + s);
                return null;
            }
        }).doWork(new Function1<Void, Void>() {
            @Override
            public Void invoke(Void aVoid) {
                Log.d("dispatcherTest", "void method called");
                return null;
            }
        }).run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dispatchController.cancelAllDispatch();
    }

}
