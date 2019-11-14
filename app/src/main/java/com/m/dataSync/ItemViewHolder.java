package com.m.dataSync;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.m.androidNativeApp.R;

class ItemViewHolder extends RecyclerView.ViewHolder {

    TextView title, description;
    Button deleteButton;

    ItemViewHolder(@NonNull View itemView) {
        super(itemView);

        title = itemView.findViewById(R.id.titleView);
        description = itemView.findViewById(R.id.descriptionView);
        deleteButton = itemView.findViewById(R.id.deleteButton);

    }
}
