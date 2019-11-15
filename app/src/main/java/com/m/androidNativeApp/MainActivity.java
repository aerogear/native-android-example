package com.m.androidNativeApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.m.services.dataSync.Client;
import com.m.helper.CreateTask;
import com.m.helper.Item;
import com.m.helper.ItemAdapter;
import com.m.androidNativeApp.fragment.TaskFields;
import com.m.helper.LoginActivity;
import com.m.push.NotifyingHandler;
import com.m.push.PushApplication;


import org.jboss.aerogear.android.core.Callback;
import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.RegistrarManager;
import org.jboss.aerogear.android.unifiedpush.fcm.UnifiedPushMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static com.m.services.appAuth.AppAuthActivity.RE_AUTH;
import static com.m.services.appAuth.AppAuthActivity.mAuthStateManager;
import static com.m.services.appAuth.AppAuthActivity.mobileService;



public class MainActivity extends AppCompatActivity implements MessageHandler {

    public static ApolloClient client;
    private String taskTitle, taskDescription, taskId;
    private RecyclerView recyclerView;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    /**
     * This function covers initiating client and running initial getTasks query to populate our
     * view. We also subscribe to addTask and deleteTask mutations.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupClient();
        setupPush();

        itemAdapter = getAdapter();
        MainActivity.this.runOnUiThread(() -> recyclerView.setAdapter(itemAdapter));

        getTasks();
        subscribeToAddTask();
        subscribeToDeleteTask();
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


    /**
     * This is a method that is used in order to get all the data from the GraphQL server, first
     * we are performing a simple check to use different network policy depending on our device being in online or
     * offline mode then creating a tasksQuery and using Apollo Client to execute the query.
     */
    public void getTasks() {

        ResponseFetcher onlineResponse = ApolloResponseFetchers.NETWORK_ONLY;

        if (!isOnline()) {
            onlineResponse = ApolloResponseFetchers.CACHE_ONLY;
        }

        AllTasksQuery tasksQuery = AllTasksQuery
                .builder()
                .build();

        client.query(tasksQuery)
                .responseFetcher(onlineResponse)
                .enqueue(new ApolloCall.Callback<AllTasksQuery.Data>() {

                    /**
                     * If the response was successful we can fill our item list data with data
                     * receivec from the server.
                     * @param response
                     *          This is the response from server that contains all the data we have
                     *          asked for in our tasksQuery.
                     */
                    @Override
                    public void onResponse(@NotNull Response<AllTasksQuery.Data> response) {
                        final int dataLength = response.data().allTasks().size();
                        for (int i = 0; i < dataLength; i++) {
                            TaskFields dataReceived = response.data().allTasks().get(i).fragments().taskFields();
                            taskTitle = dataReceived.title();
                            taskDescription = dataReceived.description();
                            taskId = dataReceived.id();
                            itemList.add(new Item(taskTitle, taskDescription, taskId));
                        }

                        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());
                    }

                    /**
                     * If we don't get anything back from the server onFailure is going to be initialized
                     * where we can handle any errors received.
                     * @param e
                     *        Error received.
                     */
                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }
                });
    }


    /**
     * Redirecting to addTaskActivity
     * @param view
     */
    public void addTaskActivity(View view) {
        Intent launchActivity1 = new Intent(this, CreateTask.class);
        startActivity(launchActivity1);
    }

    /**
     * This is our main DeleteTask function, on creation of each task each delete button has been
     * assigned a TAG equal to the taskID which autoincrement by the GraphQL server. AllTasksQuery
     * is rebuild here just to be accessible by refetchQueries used in deleteTask mutation builder.
     * Next, building deleteTask mutation that is going to be send to the GraphQL server and using
     * Apollo Client to execute mutation.
     * @param view
     */
    public void deleteTask(View view) {

        final Button button = view.findViewById(R.id.deleteButton);

        final String buttonId = button.getTag().toString();

        AllTasksQuery tasksQuery = AllTasksQuery
                .builder()
                .build();
        client.query(tasksQuery);

        DeleteTaskMutation deleteTask = DeleteTaskMutation
                .builder()
                .id(buttonId)
                .build();

        client.mutate(deleteTask)
                .refetchQueries(tasksQuery)
                .enqueue(new ApolloCall.Callback<DeleteTaskMutation.Data>() {

                    /**
                     * If deleteTask mutation was successful we can delete data from our itemList.
                     * @param response
                     *          This is the response from server that contains all the data we have
                     *          asked after our deleteTask has completed.
                     */
                    @Override
                    public void onResponse(@NotNull final Response<DeleteTaskMutation.Data> response) {

                        for (Item item : itemList) {
                            if (response.data() != null && item.getId().equals(response.data().deleteTask())) {
                                itemList.remove(item);
                                break;
                            }
                        }

                        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());

                    }


                    /**
                     * If we don't get anything back from the server onFailure is going to be initialized
                     * where we can handle any errors received.
                     * @param e
                     *        Error received.
                     */
                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }
                });
    }


    /**
     * Subscription to deleteTask mutations. First, building our subscription that is going to be send to
     * GraphQL server and then using build subscription to subscribe to deleteTask mutation.
     */
    public void subscribeToDeleteTask() {

        DeleteTaskSubscription deleteTaskSubscription = DeleteTaskSubscription
                .builder()
                .build();

        client.subscribe(deleteTaskSubscription)
                .execute(new ApolloSubscriptionCall.Callback<DeleteTaskSubscription.Data>() {

                    /**
                     * If deleteTaskSubscription was successful we can delete data from our itemList.
                     * @param response
                     *        This is the response from server that contains all the data we have
                     *        asked after our deleteTaskSubscription had received data.
                     */
                    @Override
                    public void onResponse(@NotNull Response<DeleteTaskSubscription.Data> response) {

                        for (Item item : itemList) {
                            if (item.getId().equals(response.data().taskDeleted.fragments().taskFields.id())) {
                                itemList.remove(item);
                                break;
                            }
                        }


                        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());

                    }


                    /**
                     * If we don't get anything back from the server onFailure is going to be initialized
                     * where we can handle any errors received.
                     * @param e
                     *        Error received.
                     */
                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }

                    /**
                     * onCompleted is triggered once subscription has been completed.
                     */
                    @Override
                    public void onCompleted() {
                        System.out.println("Subscribed to DeleteTask");
                    }

                    /**
                     * onTerminated is triggered once subscription has been terminated.
                     */
                    @Override
                    public void onTerminated() {
                        System.out.println("DeleteTask subscription terminated");
                    }

                    /**
                     * onConnected is triggered once we have connected to subscriptions.
                     */
                    @Override
                    public void onConnected() {
                        System.out.println("Connected to DeleteTask subscription");
                    }
                });
    }


    /**
     * Subscription to addTask mutations. First, building our subscription that is going to be send to
     * GraphQL server and then using build subscription to subscribe to deleteTask mutation.
     */
    public void subscribeToAddTask() {

        AddTaskSubscription addTaskSubscription = AddTaskSubscription
                .builder()
                .build();

        client.subscribe(addTaskSubscription)
                .execute(new ApolloSubscriptionCall.Callback<AddTaskSubscription.Data>() {
                    /**
                     * If addTaskSubscription was successful we can add data from our itemList.
                     * @param response
                     *        This is the response from server that contains all the data we have
                     *        asked after our addTaskSubscription had received data.
                     */
                    @Override
                    public void onResponse(@NotNull Response<AddTaskSubscription.Data> response) {

                        TaskFields dataReceived = response.data().taskAdded().fragments().taskFields;
                        itemList.add(new Item(dataReceived.title(), dataReceived.description(), dataReceived.id()));


                        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());
                    }


                    /**
                     * If we don't get anything back from the server onFailure is going to be initialized
                     * where we can handle any errors received.
                     * @param e
                     *        Error received.
                     */
                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }

                    /**
                     * onCompleted is triggered once subscription has been completed.
                     */
                    @Override
                    public void onCompleted() {
                        System.out.println("Subscribed to AddTask");
                    }

                    /**
                     * onTerminated is triggered once subscription has been terminated.
                     */
                    @Override
                    public void onTerminated() {
                        System.out.println("AddTask subscription terminated");
                    }

                    /**
                     * onConnected is triggered once we have connected to subscriptions.
                     */
                    @Override
                    public void onConnected() {
                        System.out.println("Connected to AddTask subscription");
                    }
                });

    }


    /**
     * This is called when we receive a 403 error from the server due to being logged out on our SSO
     * or token is expired. Once called, user is redirected to AppAuth Activity.
     */
    public void reAuthorise() {
        RE_AUTH = 403;
        Intent redirectToRefreshToken = new Intent(MainActivity.this, AppAuthActivity.class);
        startActivity(redirectToRefreshToken);
    }


    /**
     * Our clients accepts a serverUrl, a token and application context to interact with so we need
     * to pass these in our setupApollo method
     */
    public void setupClient() {
        String token = "Bearer " + mAuthStateManager.getCurrent().getAccessToken();
        client = Client.setupApollo(mobileService.getGraphqlServer(), token, getApplicationContext());
    }


    /**
     * Setting up the itemAdapter.
     * @return
     *    Returns itemAdater.
     */
    public ItemAdapter getAdapter() {
        itemList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(this, itemList);

        return itemAdapter;
    }


    /**
     * A check called before getTasks() to check whether device is in online of offline mode.
     */
    public boolean isOnline() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Register application with Push service
     */
    private void setupPush(){
        PushApplication application = (PushApplication) getApplication();
        PushRegistrar pushRegistrar = application.getPushRegistrar();
        pushRegistrar.register(getApplicationContext(), new Callback<Void>() {

            /**
             * Handle a successful registration with Push service
             */
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "Registration Succeeded");
            }

            /**
             * Handle a failed registration with Push service
             */
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(getApplicationContext(),
                        "Ops, something is wrong :(", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Sample method to handle the notifications when the app has been started.
     */
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
