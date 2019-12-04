package com.m.models;

import com.m.services.dataSync.fragment.TaskFields;

public class Item {

    private String title, description, id;


    public Item(String title, String description, String id) {
        this.title = title;
        this.description = description;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public static Item createNewTaskToItems(TaskFields task) {
        String taskTitle = task.title();
        String taskDescription = task.description();
        String taskId = task.id();
        return new Item(taskTitle, taskDescription, taskId);

    }
}
