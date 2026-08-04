package com.schoolforum.app.utils;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView间距装饰器
 */
@SuppressWarnings("unused")
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private final int space;
    private final boolean includeEdge;
    
    public SpacesItemDecoration(int space) {
        this(space, true);
    }
    
    public SpacesItemDecoration(int space, boolean includeEdge) {
        this.space = space;
        this.includeEdge = includeEdge;
    }
    
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        
        if (includeEdge) {
            outRect.left = space;
            outRect.right = space;
            outRect.top = position == 0 ? space : 0;
            outRect.bottom = space;
        } else {
            outRect.left = position % 2 == 0 ? space : space / 2;
            outRect.right = position % 2 == 0 ? space / 2 : space;
            outRect.top = position < 2 ? space : 0;
            outRect.bottom = space;
        }
    }
}
