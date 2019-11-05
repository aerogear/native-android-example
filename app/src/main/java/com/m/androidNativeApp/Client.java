package com.m.androidNativeApp;

import android.content.Context;

import com.apollographql.apollo.ApolloClient;
import com.m.helpper.MobileService;

import okhttp3.OkHttpClient;

public class Client {
    private static String SERVER_URL;
    private MobileService mobileService = MobileService.getInstance();

    public ApolloClient setupApollo() {

        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .build();

        System.out.println("APP: In Client: url: " + mobileService.getGraphqlServer());
        return ApolloClient.builder()
                .serverUrl(mobileService.getGraphqlServer())
                .okHttpClient(okHttpClient)
                .build();
    }
}
