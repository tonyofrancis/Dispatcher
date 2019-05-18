package com.tonyodev.dispatchandroid;

import com.tonyodev.dispatch.DispatchQueue;
import com.tonyodev.dispatchandroid.thread.AndroidThreadHandlerFactory;
import com.tonyodev.dispatchandroid.utils.AndroidLogger;

public final class AndroidFactoriesInitializer {

    private AndroidFactoriesInitializer() {
        //Do not create instance of this class
    }

    public static void init() {
        DispatchQueue.Queues.getGlobalSettings().setLogger(new AndroidLogger());
        DispatchQueue.Queues.getGlobalSettings().setThreadHandlerFactory(new AndroidThreadHandlerFactory());
    }

}
