package com.m.helper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.m.androidNativeApp.CreateTaskMutation;
import com.m.androidNativeApp.MainActivity;
import com.m.androidNativeApp.R;

import org.jetbrains.annotations.NotNull;

import static com.m.androidNativeApp.MainActivity.client;
import static com.m.helper.LoginActivity.RE_AUTH;


public class CreateTask extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_task);
    }

    public void createTask(View view) {
        EditText taskTitle = findViewById(R.id.title_input);
        EditText taskDescription = findViewById(R.id.description_input);

        CreateTaskMutation createTask = CreateTaskMutation
                .builder()
                .taskTitle(taskTitle.getText().toString())
                .taskDescription(taskDescription.getText().toString())
                .build();

        client.mutate(createTask).enqueue(new ApolloCall.Callback<CreateTaskMutation.Data>() {
            @Override
            public void onResponse(@NotNull Response<CreateTaskMutation.Data> response) {
                System.out.println("Success");
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                if (e.getMessage().equals("HTTP 403 Forbidden")) {
                    RE_AUTH = 403;
                    Intent redirectToRefreshToken = new Intent(CreateTask.this, LoginActivity.class);
                    startActivity(redirectToRefreshToken);
                }
            }
        });

        Intent redirectToRefreshToken = new Intent(CreateTask.this, MainActivity.class);
        startActivity(redirectToRefreshToken);
    }
}
