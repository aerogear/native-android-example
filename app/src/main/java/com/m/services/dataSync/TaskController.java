package com.m.services.dataSync;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ApolloResponseFetchers;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.m.models.Item;
import com.m.services.auth.LoginController;
import com.m.services.dataSync.fragment.TaskFields;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class TaskController {
    private ApolloClient client;
    private LoginController loginController;
    private Context context;
    private TaskListener listener;


    public TaskController(Context context){
        client = Client.getInstance();
        loginController = new LoginController(context);
        this.context = context;
    }

    public TaskController(Context context, TaskListener listener) {
        this(context);
        this.listener = listener;

    }

    public void getTasks(){
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
                        ArrayList<Item> itemList = new ArrayList<>();
                        for (int i = 0; i < dataLength; i++) {
                            TaskFields dataReceived = response.data().allTasks().get(i).fragments().taskFields();
                            itemList.add(Item.createNewTaskToItems(dataReceived));
                        }
                        listener.addAllTasks(itemList);
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        reAuthoriseOn403WithApollo(e);
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
                        listener.addTask(new Item(dataReceived.title(), dataReceived.description(), dataReceived.id()));

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
    public void subscribeToDeleteTask() {

        DeleteTaskSubscription deleteTaskSubscription = DeleteTaskSubscription
                .builder()
                .build();

        client.subscribe(deleteTaskSubscription)
                .execute(new ApolloSubscriptionCall.Callback<DeleteTaskSubscription.Data>() {
                    @Override
                    public void onResponse(@NotNull Response<DeleteTaskSubscription.Data> response) {

                        listener.deleteTask(response.data().taskDeleted.fragments().taskFields.id());
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

    public void deleteTask(String buttonId){
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
                        listener.deleteTask(response.data().deleteTask());
                    }

                    @Override
                    public void onFailure(@NotNull ApolloException e) {
                        reAuthoriseOn403WithApollo(e);
                    }
                });
    }

    public void createTask(String title, String description){
        CreateTaskMutation createTask = CreateTaskMutation
                .builder()
                .taskTitle(title)
                .taskDescription(description)
                .build();

        client.mutate(createTask).enqueue(new ApolloCall.Callback<CreateTaskMutation.Data>() {
            @Override
            public void onResponse(@NotNull Response<CreateTaskMutation.Data> response) {
                System.out.println("Success");
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                reAuthoriseOn403WithApollo(e);
            }
        });
    }

    private boolean isOnline() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connManager != null) {
            networkInfo = connManager.getActiveNetworkInfo();
        }

        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }


    private void reAuthoriseOn403WithApollo(ApolloException e){
        if (e.getMessage().equals("HTTP 403 Forbidden")) {
            loginController.reAuthorise();
        }
    }
}
