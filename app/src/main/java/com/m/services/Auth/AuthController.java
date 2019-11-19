package com.m.services.Auth;

import android.content.Context;
import android.content.Intent;

import static com.m.services.Auth.LoginActivity.RE_AUTH;

public class AuthController {
    private Context context;

    public AuthController(Context context){
        this.context = context;
    }

    public void reAuthorise() {
        RE_AUTH = 403;
        Intent redirectToRefreshToken = new Intent(context, LoginActivity.class);
        context.startActivity(redirectToRefreshToken);
    }
}
