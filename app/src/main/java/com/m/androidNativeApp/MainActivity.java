package com.m.androidNativeApp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.widget.Toast;

import com.m.services.push.NotifyingHandler;
import com.m.services.push.PushApplication;



import org.jboss.aerogear.android.core.Callback;
import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.RegistrarManager;
import org.jboss.aerogear.android.unifiedpush.fcm.UnifiedPushMessage;

import com.m.services.dataSync.Client;
import com.m.services.dataSync.TaskActivity;


import static com.m.services.auth.LoginActivity.mAuthStateManager;
import static com.m.services.auth.LoginActivity.mobileService;


public class MainActivity extends AppCompatActivity implements MessageHandler {



    /**
     * This function covers initiating client and running initial getTasks query to populate our
     * view. We also subscribe to addTask and deleteTask mutations.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * Setting up client
         */
        setupClient();
        setupPush();

        startSyncActivity();

        toastStartUpPushNotification();

    }

    @Override
    protected void onResume() {
        super.onResume();
        RegistrarManager.registerMainThreadHandler(this);
        RegistrarManager.unregisterBackgroundThreadHandler(NotifyingHandler.instance);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RegistrarManager.unregisterMainThreadHandler(this);
        RegistrarManager.registerBackgroundThreadHandler(NotifyingHandler.instance);
    }

    @Override
    public void onMessage(Context context, Bundle bundle) {
        // display the message contained in the payload
        Toast.makeText(getApplicationContext(),
                bundle.getString(UnifiedPushMessage.ALERT_KEY), Toast.LENGTH_LONG).show();

    }

    private void setupClient() {
        String token = "Bearer " + mAuthStateManager.getCurrent().getAccessToken();
        Client.init(mobileService.getGraphqlServer(), token, getApplicationContext());
    }

    private void startSyncActivity(){
        Intent intent = new Intent(this, TaskActivity.class);
        startActivity(intent);
    }

    /**
     * Register application with Push service
     */
    private void setupPush(){
        PushApplication application = (PushApplication) getApplication();
        PushRegistrar pushRegistrar = application.getPushRegistrar();
        pushRegistrar.register(getApplicationContext(), new Callback<Void>() {

            /**
             * Handle a successful registration with Push service
             */
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "Registration Succeeded");
            }

            /**
             * Handle a failed registration with Push service
             */
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(getApplicationContext(),
                        "Ops, something is wrong :(", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sample method to handle the notifications when the app has been started.
     */
    public void toastStartUpPushNotification(){
        if (getIntent() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null && bundle.getString(UnifiedPushMessage.ALERT_KEY) != null) {
                Toast.makeText(getApplicationContext(),
                        bundle.getString(UnifiedPushMessage.ALERT_KEY), Toast.LENGTH_LONG).show();
            }

        }
    }
}
