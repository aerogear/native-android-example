package com.m.services.dataSync;

import android.content.Context;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.m.services.Auth.AuthController;

import org.jetbrains.annotations.NotNull;

import static com.m.services.Auth.LoginActivity.RE_AUTH;

public class ClientController {
    private ApolloClient client;
    private AuthController authController;


    public ClientController(Context context){
        client = Client.getInstance();
        authController = new AuthController(context);

    }

    public void createTask(String title, String description){
        CreateTaskMutation createTask = CreateTaskMutation
                .builder()
                .taskTitle(title)
                .taskDescription(description)
                .build();

        createMutate(createTask);
    }

    private void createMutate(CreateTaskMutation task){

        client.mutate(task).enqueue(new ApolloCall.Callback<CreateTaskMutation.Data>() {
            @Override
            public void onResponse(@NotNull Response<CreateTaskMutation.Data> response) {
                System.out.println("Success");
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                if (e.getMessage().equals("HTTP 403 Forbidden")) {
                    RE_AUTH = 403;
                    authController.reAuthorise();
                }
            }
        });
    }
}
