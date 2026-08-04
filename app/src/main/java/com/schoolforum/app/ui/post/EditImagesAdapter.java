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
 * 支持两种图片项：新选图片（uri）与已有图片（url，编辑模式）
 */
public class EditImagesAdapter extends RecyclerView.Adapter<EditImagesAdapter.ViewHolder> {

    /** 图片项：uri 为新选图片，url 为服务端已有图片（二选一） */
    public static class EditImageItem {
        public Uri uri;
        public String url;

        public EditImageItem(Uri uri) { this.uri = uri; }
        public EditImageItem(String url) { this.url = url; }

        public boolean isExisting() { return url != null; }
    }

    private final List<EditImageItem> images;
    private final OnImageRemoveListener removeListener;

    public interface OnImageRemoveListener {
        void onRemove(int position);
    }

    public EditImagesAdapter(List<EditImageItem> images, OnImageRemoveListener removeListener) {
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
        EditImageItem item = images.get(position);
        Context context = holder.itemView.getContext();

        if (item.isExisting()) {
            // 已有图片：url 可能是相对路径，补全 BASE_URL
            String url = item.url.startsWith("http") ? item.url
                    : com.schoolforum.app.network.ApiClient.getBaseUrl() + item.url;
            Glide.with(context)
                .load(url)
                .centerCrop()
                .into(holder.ivImage);
        } else {
            Glide.with(context)
                .load(item.uri)
                .centerCrop()
                .into(holder.ivImage);
        }

        holder.ivRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (removeListener != null && pos != RecyclerView.NO_POSITION) {
                removeListener.onRemove(pos);
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
