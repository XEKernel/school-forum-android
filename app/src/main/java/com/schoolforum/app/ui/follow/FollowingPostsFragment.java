package com.schoolforum.app.ui.follow;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.gson.Gson;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Post;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.post.PostDetailActivity;
import com.schoolforum.app.ui.post.PostsAdapter;
import com.schoolforum.app.utils.UserManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态帖子列表Fragment
 */
public class FollowingPostsFragment extends Fragment implements PostsAdapter.OnPostClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyView;

    private PostsAdapter adapter;
    private List<Post> posts;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_following_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        posts = new ArrayList<>();
        initViews(view);
        setupRecyclerView();
        loadPosts();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadPosts();
        });
    }

    private void setupRecyclerView() {
        adapter = new PostsAdapter();
        adapter.setOnPostClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();

                    if (!isLoading && hasMore && lastVisiblePosition >= totalItemCount - 3) {
                        loadMorePosts();
                    }
                }
            }
        });
    }

    private void loadPosts() {
        if (isLoading) return;

        String userId = UserManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) return;

        isLoading = true;
        if (currentPage == 1) {
            showLoading(true);
        }

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(currentPage));
        params.put("limit", "10");

        ApiClient.getInstance(requireContext()).get("/following/posts/" + userId, params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);

                        try {
                            FollowingPostsResponse response = gson.fromJson(responseBody, FollowingPostsResponse.class);
                            if (response.success) {
                                if (currentPage == 1) {
                                    posts.clear();
                                }

                                if (response.posts != null) {
                                    posts.addAll(response.posts);
                                }

                                adapter.submitList(new ArrayList<>(posts));
                                emptyView.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);

                                if (response.pagination != null) {
                                    hasMore = response.pagination.hasNext;
                                }
                            }
                        } catch (Exception e) {
                            showError("解析数据失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        showError(error);
                    });
                }
            }, null);
    }

    private void loadMorePosts() {
        currentPage++;
        loadPosts();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        if (getView() != null && getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPostClick(Post post) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), PostDetailActivity.class);
            intent.putExtra("post_id", post.getId());
            startActivity(intent);
        }
    }

    @Override
    public void onLikeClick(Post post, int position) {
        // 点赞功能复用 PostsAdapter 的逻辑
    }

    @Override
    public void onMoreClick(Post post, View anchor) {
        // 更多菜单
    }

    private static class FollowingPostsResponse {
        boolean success;
        String message;
        List<Post> posts;
        Pagination pagination;
    }

    private static class Pagination {
        int currentPage;
        int totalPages;
        boolean hasNext;
    }
}
