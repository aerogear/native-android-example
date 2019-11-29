package com.m.services.dataSync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.m.androidNativeApp.R;
import com.m.models.Item;
import com.m.services.auth.LoginController;

import java.util.ArrayList;
import java.util.List;


public class TaskActivity extends AppCompatActivity {
    private ItemAdapter itemAdapter;
    public List<Item> itemList = new ArrayList<>();
    private TaskController taskController;
    private LoginController loginController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configRecyclerViewWithItemAdapter();

        loginController = new LoginController(this);

        TaskListener listener = new TaskListener();
        listener.setListener(new TaskListener.TaskListenerCallback() {
            @Override
            public void addAllTasks(ArrayList<Item> items) {
                itemList.addAll(items);
                updateUi();
            }

            @Override
            public void deleteTask(String taskId) {
                removeTaskFromList(taskId);
                updateUi();
            }

            @Override
            public void addTask(Item task) {
                itemList.add(task);
                updateUi();
            }

            @Override
            public void onFailure(ApolloException error) {
                handleErrorExceptions(error);
            }
        });

        taskController = new TaskController(getApplicationContext(), listener);
        taskController.getTasks();
        taskController.subscribeToAddTask();
        taskController.subscribeToDeleteTask();
    }

    public void deleteTask(View view) {

        final Button button = view.findViewById(R.id.deleteButton);

        final String buttonId = button.getTag().toString();
        taskController.deleteTask(buttonId);
    }

    public void configRecyclerViewWithItemAdapter() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new ItemAdapter(this, itemList);
        recyclerView.setAdapter(itemAdapter);
    }

    public void addTaskActivity(View view) {
        Intent launchActivity1 = new Intent(this, CreateTaskActivity.class);
        startActivity(launchActivity1);
    }

    private void removeTaskFromList(String taskId){
        if (taskId == null){
            return;
        }

        for (Item item : itemList) {
            if (item.getId().equals(taskId)) {
                itemList.remove(item);
                break;
            }
        }
    }

    private void updateUi(){
        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());
    }

    private void uiToast(String message) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());

    }

    private void handleErrorExceptions(ApolloException error){
        if (error instanceof ApolloHttpException){
            handleHttpErrors((ApolloHttpException) error);
        } else {
            uiToast(error.getMessage());
        }
    }

    private void handleHttpErrors(ApolloHttpException error){
      switch (error.code()){
          case 401:
          case 403:
              loginController.reAuthorise();
              break;
          case 503:
              uiToast("Service Unavailable");
          default:
              uiToast(error.getMessage());

      }
    }

}
