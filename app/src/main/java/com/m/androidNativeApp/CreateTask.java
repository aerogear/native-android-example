package com.m.androidNativeApp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.jetbrains.annotations.NotNull;

import static com.m.androidNativeApp.MainActivity.client;

public class CreateTask extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_task);
    }

    public void createTask(View view) {
        EditText taskTitle = (EditText) findViewById(R.id.title_input);
        EditText taskDescription = (EditText) findViewById(R.id.description_input);

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

            }
        });

        finish();
    }
}
