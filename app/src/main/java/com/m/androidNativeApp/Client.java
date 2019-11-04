package com.m.androidNativeApp;

import com.apollographql.apollo.ApolloClient;

import okhttp3.OkHttpClient;

public class Client {

    private static final String SERVER_URL = "https://ionic-showcase-server-customer-a-shar-b4c5.apps.ire-85ac.open.redhat.com/graphql";

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
