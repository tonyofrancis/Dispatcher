package com.tonyodev.dispatchandroid;

import com.tonyodev.dispatch.Dispatcher;
import com.tonyodev.dispatchandroid.thread.AndroidThreadHandlerFactory;
import com.tonyodev.dispatchandroid.utils.AndroidLogger;

public final class AndroidFactoriesInitializer {

    private AndroidFactoriesInitializer() {
        //Do not create instance of this class
    }

    public static void init() {
        Dispatcher.setLogger(new AndroidLogger());
        Dispatcher.setThreadHandlerFactory(new AndroidThreadHandlerFactory());
    }


}
