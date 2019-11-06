package com.m.androidNativeApp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
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

        // setting recycler view to be vertical ( list of items 1 by 1 going top to bottom)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println(e);
            }
        });

        itemAdapter = new ItemAdapter(this, itemList);
        recyclerView.setAdapter(itemAdapter);
    }

    public void deleteTask(View view) {

        Button button = view.findViewById(R.id.deleteButton);

        System.out.println(button.getTag());

        String buttonId = button.getTag().toString();

        DeleteTaskMutation deleteTask = DeleteTaskMutation
                .builder()
                .id(buttonId)
                .build();

        client.mutate(deleteTask).enqueue(new ApolloCall.Callback<DeleteTaskMutation.Data>() {
            @Override
            public void onResponse(@NotNull Response<DeleteTaskMutation.Data> response) {
                System.out.println("Success");
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("Error");
            }
        });
    }

}
