package com.m.androidNativeApp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    // Need context object to inflate the list_layout.xml
    Context mCtx;

    // Need List of items to display list of data from server
    private List<Item> itemList;

    // constructor for Item Adapter
    public ItemAdapter(Context mCtx, List<Item> itemList) {
        this.mCtx = mCtx;
        this.itemList = itemList;
    }

    // Creates a view holder - returns an instance of ItemViewHolder class which are UI elements
    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        // creating item view instance

        // first we need to inflate the layout with data
        LayoutInflater inflater = LayoutInflater.from(mCtx);

        // select which view to inflate and parent object
        View view = inflater.inflate(R.layout.list_layout, null);

        // creating item view holder instance and passing in inflated view we have created above!
        ItemViewHolder holder = new ItemViewHolder(view);

        return holder;
    }

    // Binds data to our view holder
    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {

        // here we need to bind the data to ui elements;

        // First we need to get position of the item in the list
        Item item = itemList.get(position);

        // filling in each field for inflation
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
       holder.deleteButton.setTag(item.getId());

    }

    // Returns size of our list of items
    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // Takes in a view - list_layout which is inflated by the context
    class ItemViewHolder extends RecyclerView.ViewHolder {

        TextView title, description;
       Button deleteButton;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            // assigning title and description to UI elements title and description views.
            title = itemView.findViewById(R.id.titleView);
            description = itemView.findViewById(R.id.descriptionView);
            deleteButton = itemView.findViewById(R.id.deleteButton);

        }
    }


}
