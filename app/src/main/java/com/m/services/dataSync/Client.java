package com.m.services.dataSync;

import android.content.Context;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCache;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper;
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class Client {

    /**
     * Setting up Apollo Client
     * @param serverUrl
     *           Server URL provided in mobile-service.json, in our example passed in as a parameter
     *           in MainActivity to construct Apollo Client.
     * @param authHeader
     *           Token received.
     * @param context
     *          Context required for normalized cache.
     * @return
     *          Returns interceptor chain from OkHttp
     */
    public static ApolloClient setupApollo(String serverUrl, String authHeader, Context context) {


        /**
         * Resolver that can be used to access records from cache
         */
        CacheKeyResolver resolver = new CacheKeyResolver() {
            @NotNull
            @Override
            public CacheKey fromFieldRecordSet(@NotNull ResponseField field, @NotNull Map<String, Object> recordSet) {
                return formatCacheKey((String) recordSet.get("id"));
            }

            @NotNull
            @Override
            public CacheKey fromFieldArguments(@NotNull ResponseField field, @NotNull Operation.Variables variables) {
                return formatCacheKey((String) field.resolveArgument("id", variables));
            }

            private CacheKey formatCacheKey(String id) {
                if (id == null || id.isEmpty()) {
                    return CacheKey.NO_KEY;
                } else {
                    return CacheKey.from(id);
                }
            }
        };

        String DB_CACHE_NAME = "myapp";


        /**
         *  Creating normalized cache for our app to enable offline support for our application
         */
        ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(context, DB_CACHE_NAME);

        NormalizedCacheFactory<LruNormalizedCache> cacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
                .chain(new SqlNormalizedCacheFactory(apolloSqlHelper));


        /**
         * OkHttp is handling network requests for our client.
         */
        OkHttpClient okHttpClient = new OkHttpClient
                .Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder().method(original.method(), original.body());
                    builder.header("Authorization", authHeader);
                    return chain.proceed(builder.build());
                })
                .build();


        /**
         * Setting up authorization to support subscriptions
         */
        Map<String, Object> connectionParams = new HashMap<>();
        connectionParams.put("Authorization", authHeader);


        /**
         * Building Apollo Client, passing in serverUrl, normalized cache, okHttp client and setting up
         * subscription mechanism.
         */
        return ApolloClient.builder()
                .enableAutoPersistedQueries(true)
                .serverUrl(serverUrl)
                .normalizedCache(cacheFactory, resolver)
                .subscriptionConnectionParams(connectionParams)
                .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory(serverUrl, okHttpClient))
                .okHttpClient(okHttpClient)
                .build();

    }
}
