package com.m.dataSync;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.m.androidNativeApp.R;
import com.m.helper.Item;
import com.m.helper.LoginActivity;
import com.m.dataSync.fragment.TaskFields;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.m.helper.LoginActivity.RE_AUTH;


public class SyncActivity extends AppCompatActivity {
    public static ApolloClient client;
    private String taskTitle, taskDescription, taskId;
    private RecyclerView recyclerView;
    private ItemAdapter itemAdapter;
    public List<Item> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        client = Client.getInstance();

        itemAdapter = getAdapter();
        recyclerView.setAdapter(itemAdapter);

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
                        reAuthoriseOn403WithApollo(e);
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
                        reAuthoriseOn403WithApollo(e);
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
                        reAuthoriseOn403WithApollo(e);
                    }
                });
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
                            addNewTaskToItems(dataReceived);
                        }

                        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        reAuthoriseOn403WithApollo(e);
                    }
                });
    }

    private void addNewTaskToItems(TaskFields task){
        taskTitle = task.title();
        taskDescription = task.description();
        taskId = task.id();
        itemList.add(new Item(taskTitle, taskDescription, taskId));
    }

    private void reAuthoriseOn403WithApollo(ApolloException e){
        if (e.getMessage().equals("HTTP 403 Forbidden")) {
            reAuthorise();
        }
    }

    public void reAuthorise() {
        RE_AUTH = 403;
        Intent redirectToRefreshToken = new Intent(SyncActivity.this, LoginActivity.class);
        startActivity(redirectToRefreshToken);
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
        NetworkInfo networkInfo = null;
        if (connManager != null) {
            networkInfo = connManager.getActiveNetworkInfo();
        }

        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    public void addTaskActivity(View view) {
        Intent launchActivity1 = new Intent(this, CreateTask.class);
        startActivity(launchActivity1);
    }

}
