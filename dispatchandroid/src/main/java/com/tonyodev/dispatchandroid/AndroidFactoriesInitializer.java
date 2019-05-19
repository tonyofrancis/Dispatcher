package com.tonyodev.dispatchandroid;

import com.tonyodev.dispatch.thread.ThreadHandlerFactory;
import com.tonyodev.dispatch.utils.Logger;
import com.tonyodev.dispatchandroid.thread.AndroidThreadHandlerFactory;
import com.tonyodev.dispatchandroid.utils.AndroidLogger;

public final class AndroidFactoriesInitializer {

    private AndroidFactoriesInitializer() {
        //Do not create instance of this class
    }

    public static Logger getLogger() {
        return new AndroidLogger();
    }

    public static ThreadHandlerFactory getThreadHandlerFactory() {
        return new AndroidThreadHandlerFactory();
    }

}
