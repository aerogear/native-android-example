package com.m.androidNativeApp;

import com.apollographql.apollo.ApolloClient;
import com.m.helper.MobileService;

import okhttp3.OkHttpClient;

public class Client {
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
