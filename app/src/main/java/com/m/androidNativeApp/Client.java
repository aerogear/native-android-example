package com.m.androidNativeApp;

import com.apollographql.apollo.ApolloClient;

import okhttp3.OkHttpClient;

public class Client {

    private static final String SERVER_URL = "http://10.0.2.2:4000/graphql";

    public static ApolloClient setupApollo() {

        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .build();

        return ApolloClient.builder()
                .serverUrl(SERVER_URL)
                .okHttpClient(okHttpClient)
                .build();
    }

    public static ApolloClient client = new Client().setupApollo();
}
