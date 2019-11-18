
# Sample Android Native Application

## Introduction

This is a sample Android Java application showing use of DataSync, [Keycloak](https://www.keycloak.org/about.html) and Unified Push using native upstream SDK's.
Application is sending requests to [Ionic showcase server]([https://github.com/aerogear/ionic-showcase/tree/master/server](https://github.com/aerogear/ionic-showcase/tree/master/server)) which is a GraphQL server. For DataSync, application uses Apollo Client to query, mutate and subscribe. [AppAuth](https://github.com/openid/AppAuth-Android) to connect
 with Keycloak and [aerogear android push][https://github.com/aerogear/aerogear-android-push/tree/master] for Unified Push support.

## Implementation
### 1. DataSync
#### Generating queries, mutations and subscriptions
  To generate queries, mutations and subscriptions of running GraphQL server [Apollo Codegen]([https://github.com/apollographql/apollo-tooling](https://github.com/apollographql/apollo-tooling)) was used.

#### Creating client
 - First, we need to build OkHttpClient to handle network requests
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
MyGeneratedQuery nameOfMyQuery = MyGeneratedQuery
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
  client.query(nameOfMyQuery)
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
#### External Setup
For setting up push notifications a [firebase][https://firebase.google.com/] account is required. The following information below is required and can be found the firebase project settings.

- The `google-services.json` file.
- Sender ID 
- Server Key

With in the [Aerogear Unifiefpush Server][https://github.com/aerogear/aerogear-unifiedpush-server] create you application. For creating the application you will need the following information.

- Sender ID - This is gotten from firebase
- Server Key - this is gotten from firebase

Once the application variant has been set up the follow information is need to add to the `push-config.json` file.

- Server URL
- Variant ID
- Variant Secret
- Sender ID (This is the same sender ID as gotten from firebase)

Located in `app/src/main/assets` folder is a sample of the `push-config.json` file which has the following format. This file is auto located by the [aerogear-android-push][https://github.com/aerogear/aerogear-android-push] package *(this can be configured)*.

```json

{
  "pushServerURL": "pushServerURL (e.g http(s)//host:port/context)",
  "android": {
    "senderID": "senderID (e.g Google Project ID only for android)",
    "variantID": "variantID (e.g. 1234456-234320)",
    "variantSecret": "variantSecret (e.g. 1234456-234320)"
  }
}
```

#### Project Setup
- Include the [aerogear-android-push][https://github.com/aerogear/aerogear-android-push] package in `build.gradle`.  

```
dependencies {
    ...
    implementation 'org.jboss.aerogear:aerogear-android-push:5.1.0'
    ...
}
```
- Edit the `AndroidManifest.xml` to allow permissions for the push notifications and change which class the application is launched from.

```xml
<manifest>
    <uses-permission android:name="android.permission.GET_ACCOUNTS" />
    ...
    <application
        android:name="com.m.push.PushApplication"
        >
        ...
    </application>
</manifest>
```

- Create the new application launcher class. In this class the a ups registrar is setup. 
- The `onCreate` function consumes the `push-config.json` file.

```java
package com.m.push;

import android.app.Application;

import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.RegistrarManager;
import org.jboss.aerogear.android.unifiedpush.fcm.AeroGearFCMPushJsonConfiguration;

public class PushApplication extends Application {

    private static final String PUSH_REGISTRAR_NAME = "myPushRegistrar";

    @Override
    public void onCreate() {
        super.onCreate();

        RegistrarManager.config(PUSH_REGISTRAR_NAME, AeroGearFCMPushJsonConfiguration.class)
                .loadConfigJson(getApplicationContext())
                .asRegistrar();

    }

    // Accessor method for Activities to access the 'PushRegistrar' object
    public PushRegistrar getPushRegistrar() {
        return RegistrarManager.getRegistrar(PUSH_REGISTRAR_NAME);
    }

}
```

- The next step would be to configure the notification handler. This class will implement the MessageHandler interface. 
- The method to look at here is `onMessage()` which handles what happens when the device receives a message. This function is called both times, when the application is in the Foreground and in the Background. 
- `notify()` sets up how the message should be displayed in the top notification area. The activity that is opened when the user clicks on the notifications is configured here. Other information such as title and icon can also be set at this time. 

```java 
package com.m.push;

public class NotifyingHandler implements MessageHandler {

    public static final int NOTIFICATION_ID = 1;
    private Context context;

    public static final NotifyingHandler instance = new NotifyingHandler();

    public NotifyingHandler() {}
    
    @Override
    public void onMessage(Context context, Bundle bundle) {
        this.context = context;

        String message = bundle.getString(UnifiedPushMessage.ALERT_KEY);

        Log.d("APP: Notification", message);

        notify(bundle);
    }

    private void notify(Bundle bundle) {
        NotificationManager mNotificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        String message = bundle.getString(UnifiedPushMessage.ALERT_KEY);

        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(PendingIntent.FLAG_UPDATE_CURRENT)
                .putExtra(UnifiedPushMessage.ALERT_KEY, message);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentText(message);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}
```

- Update the `MainActivity` class to use the new message handler and register with the Unifiedpush Server. 
- In the `onCreate()`, calling `setupPush()` registers with the Unifiedpush server.  The `setupPush()` function can handle what happens when the setup was successfully or if there was a failure. 
- The `onResume()` and `onPause()` functions are insuring that the message handler is registered while the device is in these states. 
- While the application is in the foreground the `onMessage()` function is called all the application to handle the push notification. In this example its a simple toast that is shown to the user. 
- Finally the `toastStartUpPushNotification()` is an example that is called when the user launches the application. If the application is launched by the user clicking in the push notification, that notification gets toasted to the user.

```java
public class MainActivity extends AppCompatActivity implements MessageHandler {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setupPush();
        toastStartUpPushNotification();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        RegistrarManager.registerMainThreadHandler(this);
        RegistrarManager.unregisterBackgroundThreadHandler(NotifyingHandler.instance);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RegistrarManager.unregisterMainThreadHandler(this);
        RegistrarManager.registerBackgroundThreadHandler(NotifyingHandler.instance);
    }

    @Override
    public void onMessage(Context context, Bundle bundle) {
        // display the message contained in the payload
        Toast.makeText(getApplicationContext(),
                bundle.getString(UnifiedPushMessage.ALERT_KEY), Toast.LENGTH_LONG).show();

    }
    
    private void setupPush(){
        PushApplication application = (PushApplication) getApplication();
        PushRegistrar pushRegistrar = application.getPushRegistrar();
        pushRegistrar.register(getApplicationContext(), new Callback<Void>() {
            
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "Registration Succeeded");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(getApplicationContext(),
                        "Ops, something is wrong :(", Toast.LENGTH_LONG).show();
            }
        });
    }
    
    public void toastStartUpPushNotification(){
        if (getIntent() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null && bundle.getString(UnifiedPushMessage.ALERT_KEY) != null) {
                Toast.makeText(getApplicationContext(),
                        bundle.getString(UnifiedPushMessage.ALERT_KEY), Toast.LENGTH_LONG).show();
            }

        }
    }

}
```

More information can be found at [docs.aerogear.org][https://docs.aerogear.org/aerogear/latest/push-notifications.html].

 
### 4. Mobile-services parser
