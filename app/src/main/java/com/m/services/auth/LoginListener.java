package com.m.services.auth;

import android.content.Intent;

import com.m.models.Item;

import java.util.ArrayList;

public class LoginListener {

    public interface LoginListenerCallback {
        public void reAuth(Intent intent);
        public void authComplete();
    }

    private LoginListenerCallback listener;

    public LoginListener() {
        this.listener = null;
    }

    public void setListener(LoginListenerCallback listener) {
        this.listener = listener;
    }

    public void reAuth(Intent intent){
        listener.reAuth(intent);
    }

    public void authComplete(){
        listener.authComplete();
    }
}
