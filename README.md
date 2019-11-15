
# Sample Android Native Application

## Introduction

This is a sample Android Java application showing use of DataSync, [Keycloak](https://www.keycloak.org/about.html) and Unified Push using native upstream SDK's. Application is sending requests to [Ionic showcase server]([https://github.com/aerogear/ionic-showcase/tree/master/server](https://github.com/aerogear/ionic-showcase/tree/master/server)) which is a GraphQL server. 

- For DataSync, application uses Apollo Client to query, mutate and subscribe. 
- For authorization we are using [AppAuth](https://github.com/openid/AppAuth-Android) to connect
 with Keycloak. 
- For Unifiedpush support we are using Aerogear SDK.

## Implementation
### 1. DataSync
#### Generating queries, mutations and subscriptions
  To generate queries, mutations and subscriptions of running GraphQL server [Apollo Codegen]([https://github.com/apollographql/apollo-tooling](https://github.com/apollographql/apollo-tooling)) was used.

#### Creating client
 - First, we need to build OkHttpClient to handle network requests.
`authHeader` is the actual token received from the token request during authorization flow. `authHeader` must contain `Bearer:` + token value:  `"Bearer: TOKEN"`
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
 - Then, NormalizedCacheFactory to provide caching mechanism
 ```java
 ApolloSqlHelper apolloSqlHelper = new ApolloSqlHelper(context, DB_CACHE_NAME);

NormalizedCacheFactory<LruNormalizedCache> cacheFactory = new LruNormalizedCacheFactory(EvictionPolicy.NO_EVICTION)
        .chain(new SqlNormalizedCacheFactory(apolloSqlHelper));
 ```
 - Passing in connection params for subscriptions to work if server is behind authentication mechanism
 ```java
 Map<String, Object> connectionParams = new HashMap<>();
connectionParams.put("Authorization", authHeader);
```
- And build our Apollo Client
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
#### Using queries, mutation and subscriptions
Once client is build we can use it to run queries, mutations and subscriptions. On application start, a query, mutation or subscription is build
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

   Next step is to use created client to initialize builded query, mutation or subscription and send request to the server
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
- Currently the only way to refresh cache after receiving subscription response is to use `subscribeToMore`, however, this is not available in java.
  After request has been sent we either receive `onResponse`, if request was successful, or `onFailure` if request has failed.

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
### 2. Keycloak implementation
To implement Keycloak with our app we have used [AppAuth]([https://github.com/openid/AppAuth-Android](https://github.com/openid/AppAuth-Android)). You will need a keycloak instance running either on OpenShift or you can setup locally on Ionic Showcase server that has been used in our example app

- First step is to fetch well knows configuration for Keycloak
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
- Once we have received configuration from Keycloak we can build Authorization Request
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
- It is important to note that we need to provide valid `redirect_URI` and `clientId`, in our sample app we are using `mobile-services.json` file that has been generated in [MDC]([https://access.redhat.com/documentation/en-us/red_hat_mobile_developer_services/1/pdf/getting_started/Red_Hat_Mobile_Developer_Services-1-Getting_Started-en-US.pdf](https://access.redhat.com/documentation/en-us/red_hat_mobile_developer_services/1/pdf/getting_started/Red_Hat_Mobile_Developer_Services-1-Getting_Started-en-US.pdf)). Once authorization request has been send app is redirected to Keycloak log in screen.
- After logging in with valid credentials and providing valid `redirect_URI` user is pushed back to the application and can retrieve response and can update state in auth state manager if wish to do so.
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
- With the response back we can trigger `exchangeAuthorizationCode` and perform token request
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
- Once we get a response we can view our token details
```java
private void receivedTokenResponse(
        @Nullable TokenResponse tokenResponse,
  @Nullable AuthorizationException authException) {
    mAuthState.update(tokenResponse, authException);

  System.out.println("Token expires in : " + mAuthState.getAccessTokenExpirationTime());

  startActivity(new Intent(this, MainActivity.class));
}
```
### 3. Unifiedpush implementation
### 4. Mobile-services parser
