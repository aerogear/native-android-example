
# Sample Android Native Application

## Introduction

This is a sample Android Java application showing use of DataSync, [Keycloak](https://www.keycloak.org/about.html) and Unified Push using native upstream SDK's. Backend is covered by a GraphQL server - [Ionic showcase server](https://github.com/aerogear/ionic-showcase/tree/master/server). 

- For DataSync, application uses [Apollo Client](https://www.apollographql.com/docs/android/essentials/get-started/) to query, mutate and subscribe. 
- For authorization we are using [AppAuth](https://github.com/openid/AppAuth-Android) to connect
 with Keycloak. 
- For Unifiedpush support we are using Aerogear SDK.

## DataSync

### Dependencies Required

In your  `build.gradle` file add:

- Plugin:

```java
apply plugin: 'com.apollographql.android'
```
- Dependencies:
```java
implementation 'com.apollographql.apollo:apollo-runtime:1.2.0'
implementation 'com.apollographql.apollo:apollo-android-support:1.2.0'
implementation 'com.squareup.okhttp3:okhttp:3.12.2'
implementation 'com.apollographql.apollo:apollo-http-cache:1.2.1'
```

### Generating queries, mutations and subscriptions
  [Apollo Codegen](https://github.com/apollographql/apollo-tooling) is used to generate queries, mutations and subscriptions based off the server side schema.

### 1. Creating client

This part covers creation of the Apollo Client. To find more information about setting up an Apollo Client visit [Apollo documentation](https://www.apollographql.com/docs/android/essentials/get-started/).

#### OkHttpClient
 OkHttpClient is used to handle network requests. 


  ```java
  OkHttpClient okHttpClient = new OkHttpClient
        .Builder()
        .addInterceptor(chain -> {
            Request original = chain.request();
  Request.Builder builder = original.newBuilder().method(original.method(), original.body());
  builder.header("Authorization", authHeader);
  return chain.proceed(builder.build());
  })
        .build();
```
To obtain `authHeader`:
- User needs to be authorized so we can issue token request to authorization provider.
- Once token is received add `"Bearer: "` at the begining of the token string.
- When the token is in the following format: `"Bearer: TOKEN_VALUE"` pass it in to client as `authHeader`
 
#### Setting up cache for offline support 
Application is using NormalizedCacheFactory to provide caching mechanism
 
 ```java
 ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(context, DB_CACHE_NAME);

NormalizedCacheFactory<LruNormalizedCache> cacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(new SqlNormalizedCacheFactory(apolloSqlHelper));
 ```
Passing in connection params for subscriptions to work if server is behind authentication mechanism
 
 ```java
 Map<String, Object> connectionParams = new HashMap<>();
connectionParams.put("Authorization", authHeader);
```
#### Bulding Apollo Client


```java
return ApolloClient.builder()
        .enableAutoPersistedQueries(true)
        .serverUrl(serverUrl)
        .normalizedCache(cacheFactory, resolver)
        .subscriptionConnectionParams(connectionParams)
        .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory(serverUrl, okHttpClient))
        .okHttpClient(okHttpClient)
        .build();
```

### 2. Using queries, mutation and subscriptions
Client is used to run queries, mutations and subscriptions. On application start, a query, mutation or subscription are build. For more information regarding building queries, mutations and subscription follow [Apollo Documentation for Android](https://www.apollographql.com/docs/android/).
#### Query

```java
AllTasksQuery tasksQuery = AllTasksQuery
        .builder()
        .build();
  ```
#### Mutation

 ```java
DeleteTaskMutation deleteTask = DeleteTaskMutation
        .builder()
        .id(Param that is passed in to identify item to remove)
        .build();
 ```

#### Subscriptions
```java
DeleteTaskSubscription deleteTaskSubscription = DeleteTaskSubscription
        .builder()
        .build();
   ```

   Next, use created client to initialize built query, mutation, or subscription and send request to the server.
#### Query
  ```java
  client.query(tasksQuery)
        .responseFetcher(onlineResponse)
        .enqueue(new ApolloCall.Callback<MyGeneratedQuery.Data>()
  ```
- `.responseFetcher` in our example has either `NETWORK_ONLY` or `CACHE_ONLY` policy. Depending on device being online or offline. If `online` all data used in query is coming from the server, if `offline` all data is coming from normalized cache.
#### Mutation
 ```java
 client.mutate(deleteTask)
        .refetchQueries(tasksQuery)
        .enqueue(new ApolloCall.Callback<DeleteTaskMutation.Data>() {
```
- We want to use `refetchQueries(myGetQuery)` in mutations just to refetch our get query so cache gets updated.

#### Subscriptions
   ```java
   client.subscribe(deleteTaskSubscription)
        .execute(new ApolloSubscriptionCall.Callback<DeleteTaskSubscription.Data>() {
  ```
- Currently the only way to refresh cache after receiving subscription response is to use `subscribeToMore`, however, this is not available in Java.
  After request has been sent we either receive `onResponse`, if request was successful, or `onFailure` if request has failed.
  You can find example logic for `onResponse` or `onFailure` in  [Apollo documentation](https://www.apollographql.com/docs/android/essentials/queries/).

 ```java
  @Override
public void onResponse(@NotNull Response<AllTasksQuery.Data> response) {
    ON RESPONSE LOGIC
}
 @Override
public void onFailure(@NotNull ApolloException e) {
    ON FAILURE LOGIC
}
```
## Keycloak implementation
[AppAuth ](https://github.com/openid/AppAuth-Android/blob/master/README.md) was used to implement Keycloak support.
To be able to run this example app keycloak instance running either on OpenShift or locally is required. To run locally, follow instructions on [Ionic showcase server](https://github.com/aerogear/ionic-showcase/tree/master/server).

#### Dependencies Required
Add the following to your `build.gradle` file:

```java
implementation 'net.openid:appauth:0.7.1'
```

### 1. Fetching well known configuration

First step is to fetch well known configuration.

```java
mAuthService = new AuthorizationService(this);
final AuthorizationServiceConfiguration.RetrieveConfigurationCallback retrieveCallback =
        (authorizationServiceConfiguration, e) -> {
            if (e != null) {
                System.out.println("Failed to retrieve configuration for " + mobileService.getKIssuer());
  } else {
                makeAuthRequest(authorizationServiceConfiguration);
  }
        };

String discoveryEndpoint = mobileService.getKIssuer() + ".well-known/openid-configuration";
AuthorizationServiceConfiguration.fetchFromUrl(Uri.parse(discoveryEndpoint), retrieveCallback);
```
### 2. Building authorization request

Once configuration is received from Keycloak, we can build Authorization Request.

```java
String k_REDIRECT_URI = "com.m.androidnativeapp:/oauth2redirect";

AuthorizationRequest.Builder authorizationRequest = new AuthorizationRequest.Builder(
        serviceConfiguration,
  mobileService.getKClientId(),
  ResponseTypeValues.CODE,
  Uri.parse(k_REDIRECT_URI));

AuthorizationRequest authRequest = authorizationRequest.build();

Intent authIntent = mAuthService.getAuthorizationRequestIntent(authRequest);
startActivityForResult(authIntent, RC_AUTH);
```
- It is important to note that valid `redirect_URI` and `clientId` are required, in this sample app both variables are located in, you will however, need to provide your own ones. 
Once authorization request has been send, app is redirected to Keycloak log in screen. 
For more information about `redirect_URI` or `clientId` please visit [AppAuth documentation](https://github.com/openid/AppAuth-Android/blob/master/README.md).

### 3. Redirecting back to the application

After logging in with valid credentials and providing valid `redirect_URI` user is pushed back to the application and can retrieve response. Optionally, `authState` can be updated as well. For more information about `authState` visit [AppAuth documentation](https://github.com/openid/AppAuth-Android/blob/master/README.md).

```java
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
 if (requestCode == RC_AUTH) {
        AuthorizationResponse resp = AuthorizationResponse.fromIntent(data);
  AuthorizationException ex = AuthorizationException.fromIntent(data);

  OPTIONALLY UPDATE AUTH STATE MANAGER

  exchangeAuthorizationCode(resp);

  } else {
        System.out.println("Error, wrong response code");
  }
}
```

### 4. Requesting token

With the response back `exchangeAuthorizationCode` can be triggered and token request performed.

```java
private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
    performTokenRequest(authorizationResponse.createTokenExchangeRequest());
}
private void performTokenRequest(TokenRequest request) {
    mAuthService.performTokenRequest(
            request,
  (tokenResponse, ex) -> {
                receivedTokenResponse(tokenResponse, ex);
  mAuthStateManager.updateAfterTokenResponse(tokenResponse, ex);
  });
}
```
On response token details can be viewed and used to construct `authHeader` used during Apollo Client initialization.

```java
private void receivedTokenResponse(
        @Nullable TokenResponse tokenResponse,
  @Nullable AuthorizationException authException) {
    mAuthState.update(tokenResponse, authException);

  startActivity(new Intent(this, MainActivity.class));
}
```
## Unifiedpush implementation

#### Dependencies Required
Add the following to your `build.gradle` file:

```java
apply plugin: 'com.google.gms.google-services'
```

```java
implementation 'org.jboss.aerogear:aerogear-android-push:5.1.0'
```

### 1. External Setup
For setting up push notifications a [Firebase](https://firebase.google.com/) account is required. The following information below is required and can be found the firebase project settings.

- The `google-services.json` file.
- Sender ID 
- Server Key

Create application within the [Aerogear Unifiedpush Server](https://github.com/aerogear/aerogear-unifiedpush-server) . For creating the application `Sender Id` and `Server Key` are required, both values can be obtained from Firebase.

Once the application variant has been set, add to the `push-config.json` file.

- Server URL
- Variant ID
- Variant Secret
- Sender ID (`Sender Id` obtained from Firebase)

### 2. Project Setup

#### Setting Ups registrar

The `onCreate` function from [PushApplication.java](https://github.com/aerogear/native-android-example/blob/master/app/src/main/java/com/m/push/PushApplication.java) file consumes the `push-config.json` file.

```java
    @Override
    public void onCreate() {
        super.onCreate();

        RegistrarManager.config(PUSH_REGISTRAR_NAME, AeroGearFCMPushJsonConfiguration.class)
                .loadConfigJson(getApplicationContext())
                .asRegistrar();

    }

    public PushRegistrar getPushRegistrar() {
        return RegistrarManager.getRegistrar(PUSH_REGISTRAR_NAME);
    }
}
```

#### Configurating notification handler

Next, configure the notification handler. This class allows to control how to handle received notifications, and how the notification is displayed to the user. Code for configuration of notification handler can be viewed in [NotifyingHandler.java](https://github.com/aerogear/native-android-example/blob/master/app/src/main/java/com/m/push/NotifyingHandler.java) file.

### 3. Use of Ups registrar and message handler
Update the `MainActivity` class to use the new message handler and register with the Unifiedpush Server. Unifiedpush support is setup, and initialized in [MainActivity.java](https://github.com/aerogear/native-android-example/blob/master/app/src/main/java/com/m/androidNativeApp/MainActivity.java). 



