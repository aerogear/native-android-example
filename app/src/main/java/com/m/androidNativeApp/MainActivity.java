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
import com.m.helper.Client;
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

import static com.m.helper.LoginActivity.RE_AUTH;
import static com.m.helper.LoginActivity.mAuthStateManager;
import static com.m.helper.LoginActivity.mobileService;



public class MainActivity extends AppCompatActivity implements MessageHandler {

    public static ApolloClient client;
    private String taskTitle, taskDescription, taskId;
    private RecyclerView recyclerView;
    private ItemAdapter itemAdapter;
    private List<Item> itemList;

    private String TAG = "Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupClient();

        itemAdapter = getAdapter();
        PushApplication application = (PushApplication) getApplication();
        PushRegistrar pushRegistar = application.getPushRegistar();
        pushRegistar.register(getApplicationContext(), new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                Log.d(TAG, "Registration Succeeded");
                Toast.makeText(getApplicationContext(),
                        "Yay, Device registered", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, e.getMessage(), e);
                Toast.makeText(getApplicationContext(),
                        "Ops, something is wrong :(", Toast.LENGTH_LONG).show();
            }
        });

        
        itemList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        itemAdapter = new ItemAdapter(this, itemList);

        MainActivity.this.runOnUiThread(() -> recyclerView.setAdapter(itemAdapter));

        getTasks();
        subscribeToAddTask();
        subscribeToDeleteTask();
    }

    public void subscribeToDeleteTask() {

        DeleteTaskSubscription deleteTaskSubscription = DeleteTaskSubscription
                .builder()
                .build();

        client.subscribe(deleteTaskSubscription)
                .execute(new ApolloSubscriptionCall.Callback<DeleteTaskSubscription.Data>() {
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

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Subscribed to DeleteTask");
                    }

                    @Override
                    public void onTerminated() {
                        System.out.println("DeleteTask subscription terminated");
                    }

                    @Override
                    public void onConnected() {
                        System.out.println("Connected to DeleteTask subscription");
                    }
                });
    }

    public void subscribeToAddTask() {


        AddTaskSubscription addTaskSubscription = AddTaskSubscription
                .builder()
                .build();

        client.subscribe(addTaskSubscription)
                .execute(new ApolloSubscriptionCall.Callback<AddTaskSubscription.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<AddTaskSubscription.Data> response) {

                        TaskFields dataReceived = response.data().taskAdded().fragments().taskFields;
                        itemList.add(new Item(dataReceived.title(), dataReceived.description(), dataReceived.id()));


                        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("Subscribed to AddTask");
                    }

                    @Override
                    public void onTerminated() {
                        System.out.println("AddTask subscription terminated");
                    }

                    @Override
                    public void onConnected() {
                        System.out.println("Connected to AddTask subscription");
                    }
                });

    }

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

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }
                });
    }

    public void addTaskActivity(View view) {
        Intent launchActivity1 = new Intent(this, CreateTask.class);
        startActivity(launchActivity1);
    }

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

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        if (e.getMessage().equals("HTTP 403 Forbidden")) {
                            reAuthorise();
                        }
                    }
                });
    }

    public void reAuthorise() {
        RE_AUTH = 403;
        Intent redirectToRefreshToken = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(redirectToRefreshToken);
    }

    public void setupClient() {
        String token = "Bearer " + mAuthStateManager.getCurrent().getAccessToken();
        client = Client.setupApollo(mobileService.getGraphqlServer(), token, getApplicationContext());
    }

    public ItemAdapter getAdapter() {
        itemList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(this, itemList);

        return itemAdapter;
    }

    public boolean isOnline() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        RegistrarManager.registerMainThreadHandler(this); // 1
        RegistrarManager.unregisterBackgroundThreadHandler(NotifyingHandler.instance);
    }

    @Override
    protected void onPause() {
        super.onPause();
        RegistrarManager.unregisterMainThreadHandler(this); // 2
        RegistrarManager.registerBackgroundThreadHandler(NotifyingHandler.instance);
    }

    @Override
    public void onMessage(Context context, Bundle bundle) {
        // display the message contained in the payload
        Toast.makeText(getApplicationContext(),
                bundle.getString(UnifiedPushMessage.ALERT_KEY), Toast.LENGTH_LONG).show();

    }

}
