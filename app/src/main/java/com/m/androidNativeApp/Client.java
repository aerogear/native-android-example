package com.m.androidNativeApp;

import com.apollographql.apollo.ApolloClient;

import okhttp3.OkHttpClient;

public class Client {

    public ApolloClient setupApollo(String serverUrl) {

        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .build();

        System.out.println("APP: In Client: url: " + serverUrl);
        return ApolloClient.builder()
                .serverUrl(serverUrl)
                .okHttpClient(okHttpClient)
                .build();
    }
}
