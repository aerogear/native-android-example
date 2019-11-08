package com.m.helper;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Client {

    public static ApolloClient setupApollo(String serverUrl, String authHeader) {


        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder().method(original.method(), original.body());
                    builder.header("Authorization", authHeader);
                    return chain.proceed(builder.build());
                })
                .build();

        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put("Authorization", authHeader);

        return ApolloClient.builder()
                .serverUrl(serverUrl)
                .subscriptionConnectionParams(connectionParams)
                .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory(serverUrl, okHttpClient))
                .okHttpClient(okHttpClient)
                .build();

    }
}
