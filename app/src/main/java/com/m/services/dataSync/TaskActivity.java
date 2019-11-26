package com.m.services.dataSync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
            public void onFailure(String errorMessage) {
                if (errorMessage.equals("HTTP 403 Forbidden")) {
                    loginController.reAuthorise();
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show());
                }
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

}
