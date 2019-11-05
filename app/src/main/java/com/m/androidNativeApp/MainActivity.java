package com.m.androidNativeApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;
import android.widget.EditText;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.m.helper.MobileService;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    public MobileService mobileService;
    private ApolloClient client;
    //creating instance of Item Adapter Class with recycler view

    private String taskTitle, taskDescription, taskId;
    RecyclerView recyclerView;
    ItemAdapter itemAdapter;
    List<Item> itemList;
    Item itemToRemove;


    public Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        mobileService = MobileService.getInstance(context.getApplicationContext());

        // mobileServices needs to be created before the Apollo client can be created.
        client = new Client().setupApollo(mobileService.getGraphqlServer());

        // initialize item list
        itemList = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        itemAdapter = new ItemAdapter(this, itemList);


        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(itemAdapter);
            }
        });

        getTasks();
        subscribeToAddTask();
        subscribeToDeleteTask();
    }

    public void subscribeToDeleteTask() {

        DeleteTaskSubscription deleteTaskSubscription = DeleteTaskSubscription
                .builder()
                .build();

        client.subscribe(deleteTaskSubscription).execute(new ApolloSubscriptionCall.Callback<DeleteTaskSubscription.Data>() {

            @Override
            public void onResponse(@NotNull Response<DeleteTaskSubscription.Data> response) {

                for (Item item : itemList) {
                    if (item.id.equals(response.data().taskDeleted.fragments().taskFields.id())) {
                        itemToRemove = item;
                    }
                }

                itemList.remove(itemToRemove);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("Failed" + e);
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
        client.subscribe(addTaskSubscription).execute(new ApolloSubscriptionCall.Callback<AddTaskSubscription.Data>() {
            @Override
            public void onResponse(@NotNull Response<AddTaskSubscription.Data> response) {
                itemList.add(new Item(response.data().taskAdded.fragments().taskFields.title(), response.data().taskAdded.fragments().taskFields.description()
                        , response.data().taskAdded.fragments().taskFields.id()));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("Failed " + e);
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

        DeleteTaskMutation deleteTask = DeleteTaskMutation
                .builder()
                .id(buttonId)
                .build();

        client.mutate(deleteTask).enqueue(new ApolloCall.Callback<DeleteTaskMutation.Data>() {
            @Override
            public void onResponse(@NotNull final Response<DeleteTaskMutation.Data> response) {

                for (Item item : itemList) {
                    if (response.data() != null && item.id.equals(response.data().deleteTask())) {
                        itemToRemove = item;
                    }
                }

                itemList.remove(itemToRemove);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });

            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println(e);
            }
        });
    }

    public void addTask(View view) {
        Intent launchActivity1 = new Intent(this, CreateTask.class);
        startActivity(launchActivity1);
    }

    public void getTasks() {

        AllTasksQuery tasksQuery = AllTasksQuery
                .builder()
                .build();

        client.query(tasksQuery).enqueue(new ApolloCall.Callback<AllTasksQuery.Data>() {
            @Override
            public void onResponse(@NotNull Response<AllTasksQuery.Data> response) {
                final AllTasksQuery.Data mResponse = response.data();

                final int dataLength = mResponse.allTasks().size();

                for (int i = 0; i < dataLength; i++) {
                    taskTitle = mResponse.allTasks().get(i).fragments().taskFields().title();
                    taskDescription = mResponse.allTasks().get(i).fragments().taskFields().description();
                    taskId = mResponse.allTasks().get(i).fragments().taskFields().id();
                    itemList.add(new Item(taskTitle, taskDescription, taskId));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        itemAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println(e);
            }
        });
    }

}
