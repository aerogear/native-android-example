package com.m.services.dataSync;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.m.androidNativeApp.R;

public class CreateTaskActivity extends Activity {
    private ClientController clientController;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_task);
        clientController = new ClientController(getApplicationContext());

    }

    public void createTask(View view) {
        EditText taskTitle = findViewById(R.id.title_input);
        EditText taskDescription = findViewById(R.id.description_input);
        String title = taskTitle.getText().toString();
        String description = taskDescription.getText().toString();

        clientController.createTask(title, description);
    }

}
