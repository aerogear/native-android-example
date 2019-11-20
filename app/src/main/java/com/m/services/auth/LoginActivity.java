package com.m.services.auth;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.m.androidNativeApp.MainActivity;
import com.m.helper.MobileService;

public class LoginActivity extends AppCompatActivity {

    public static AuthStateManager mAuthStateManager;
    public static MobileService mobileService;
    private int RC_AUTH = 100;
    public static int RE_AUTH = 0;
    private LoginController loginController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuthStateManager = AuthStateManager.getInstance(this);
        Context context = getApplicationContext();
        mobileService = MobileService.getInstance();
        mobileService.init(context.getApplicationContext());



        LoginListener listener = new LoginListener();
        listener.setListener(new LoginListener.LoginListenerCallback() {
            @Override
            public void reAuth(Intent intent) {
                startActivityForResult(intent, RC_AUTH);

            }

            @Override
            public void authComplete() {
                startMainActivity();
            }
        });

        loginController = new LoginController(this, listener, mobileService);

        if (mAuthStateManager.getCurrent().isAuthorized() && RE_AUTH == 0) {
            System.out.println("User is authorized already");
            startActivity(new Intent(this, MainActivity.class));

            finish();

        } else {
            loginController.doAuth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_AUTH) {
            loginController.handleAuthLoginResponse(data);

        } else {
            System.out.println("Error, wrong response code");
        }
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
    }
}