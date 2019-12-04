package com.m.services.dataSync;

import com.apollographql.apollo.exception.ApolloException;
import com.m.models.Item;

import java.util.ArrayList;

public class TaskListener {

    public interface TaskListenerCallback {
        public void addAllTasks(ArrayList<Item> items);
        public void deleteTask(String taskId);
        public void addTask(Item task);
        public void onFailure(ApolloException error);
    }

    private TaskListenerCallback listener;

    public TaskListener() {
        this.listener = null;
    }

    public void setListener(TaskListenerCallback listener) {
        this.listener = listener;
    }

    public void addAllTasks(ArrayList<Item> items){
        listener.addAllTasks(items);
    }

    public void deleteTask(String taskId){
        listener.deleteTask(taskId);
    }

    public void addTask(Item task){
        listener.addTask(task);
    }

    public void onFailure(ApolloException error) {
        listener.onFailure(error);
    }

}
