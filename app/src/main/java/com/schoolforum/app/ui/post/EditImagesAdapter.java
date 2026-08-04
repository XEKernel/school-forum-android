package com.schoolforum.app.ui.post;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.R;

import java.util.List;

/**
 * 编辑帖子时的图片适配器
 */
public class EditImagesAdapter extends RecyclerView.Adapter<EditImagesAdapter.ViewHolder> {

    private final List<Uri> images;
    private final OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public EditImagesAdapter(List<Uri> images, OnImageRemoveListener removeListener) {
        this.images = images;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_edit_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = images.get(position);
        Context context = holder.itemView.getContext();

        Glide.with(context)
            .load(uri)
            .centerCrop()
            .into(holder.ivImage);

        holder.ivRemove.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivRemove;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
            ivRemove = itemView.findViewById(R.id.ivRemove);
        }
    }
}
