package com.m.push;

import android.app.Application;

import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.RegistrarManager;
import org.jboss.aerogear.android.unifiedpush.fcm.AeroGearFCMPushJsonConfiguration;

public class PushApplication extends Application {

    private static final String PUSH_REGISTRAR_NAME = "myPushRegistrar";

    @Override
    public void onCreate() {
        super.onCreate();

        RegistrarManager.config(PUSH_REGISTRAR_NAME, AeroGearFCMPushJsonConfiguration.class)
                .loadConfigJson(getApplicationContext())
                .asRegistrar();

    }

    // Accessor method for Activities to access the 'PushRegistrar' object
    public PushRegistrar getPushRegistrar() {
        return RegistrarManager.getRegistrar(PUSH_REGISTRAR_NAME);
    }

}