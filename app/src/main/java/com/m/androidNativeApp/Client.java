package com.m.androidNativeApp;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

public class Client {


    public ApolloClient setupApollo(String serverUrl) {
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .build();

        Map<String, Object> connectionParams = new HashMap<>();

        System.out.println("APP: In Client: url: " + serverUrl);
        return ApolloClient.builder()
                .serverUrl(serverUrl)
                .subscriptionConnectionParams(connectionParams)
                .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory(serverUrl, okHttpClient))
                .okHttpClient(okHttpClient)
                .build();
    }
}
