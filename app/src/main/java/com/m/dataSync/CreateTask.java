package com.m.dataSync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.m.androidNativeApp.MainActivity;
import com.m.androidNativeApp.R;
import com.m.helper.LoginActivity;

import org.jetbrains.annotations.NotNull;

import static com.m.helper.LoginActivity.RE_AUTH;


public class CreateTask extends Activity {
    private ApolloClient client;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        client = Client.getInstance();
        setContentView(R.layout.add_task);

    }

    /**
     * Create task mutation is being build with passed in Params of taskTitle, taskDescription
     * and then passed in to Apollo Client to execute the mutations and redirect back to main activity.
     * @param view
     */
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
                    Intent redirectToRefreshToken = new Intent(CreateTask.this, AppAuthActivity.class);
                    startActivity(redirectToRefreshToken);
                }
            }
        });

        Intent redirectToRefreshToken = new Intent(CreateTask.this, MainActivity.class);
        startActivity(redirectToRefreshToken);
    }
}
