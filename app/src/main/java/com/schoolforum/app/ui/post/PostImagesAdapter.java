package com.schoolforum.app.ui.post;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.ui.preview.ImagePreviewActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 帖子图片网格适配器
 */
public class PostImagesAdapter extends RecyclerView.Adapter<PostImagesAdapter.ViewHolder> {

    private final List<String> images;
    private final Context context;

    public PostImagesAdapter(Context context, List<String> images) {
        this.context = context;
        this.images = images != null ? images : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position < 0 || position >= images.size()) {
            return;
        }
        
        String imageUrl = images.get(position);
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        // 处理相对路径
        String fullUrl = imageUrl;
        if (!imageUrl.startsWith("http")) {
            fullUrl = MainActivity.getBaseUrl() + imageUrl;
        }

        Glide.with(context)
                .load(fullUrl)
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(holder.ivImage);

        // 点击查看大图
        holder.ivImage.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || adapterPosition < 0 || adapterPosition >= images.size()) {
                return;
            }
            
            android.content.Intent intent = new android.content.Intent(context, ImagePreviewActivity.class);
            intent.putStringArrayListExtra("images", new ArrayList<>(images));
            intent.putExtra("position", adapterPosition);
            
            // 添加 FLAG_ACTIVITY_NEW_TASK 如果 context 不是 Activity
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivImage);
        }
    }
}
