package com.schoolforum.app.ui.post;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.schoolforum.app.MainActivity;
import com.schoolforum.app.R;
import com.schoolforum.app.model.Announcement;
import com.schoolforum.app.model.Post;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.utils.SpacesItemDecoration;
import com.schoolforum.app.utils.UserManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 帖子列表Fragment
 */
@SuppressWarnings("unused")
public class PostsFragment extends Fragment implements PostsAdapter.OnPostClickListener {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private View emptyView;
    private EditText etSearch;
    private CircleImageView ivAvatar;
    private TextView tvNotificationBadge;
    private TextView tvMessageBadge;

    // 公告相关
    private LinearLayout announcementBanner;
    private MaterialCardView announcementCard;
    private ImageView ivAnnouncementIcon;
    private TextView tvAnnouncementTitle;
    private TextView tvAnnouncementContent;
    private ImageView btnCloseAnnouncement;
    private Announcement currentAnnouncement;

    private PostsAdapter adapter;
    private List<Post> posts;
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String searchQuery;
    private String sortBy = "latest";
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        posts = new ArrayList<>();
        initViews(view);
        setupRecyclerView();
        loadAnnouncements();
        loadPosts();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 返回时刷新帖子列表（处理删除/编辑后的情况）
        if (posts != null && !posts.isEmpty()) {
            refreshPosts();
        }
    }
    
    private void refreshPosts() {
        currentPage = 1;
        hasMore = true;
        loadPosts();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        etSearch = view.findViewById(R.id.etSearch);
        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);

        // 公告相关组件
        announcementBanner = view.findViewById(R.id.announcementBanner);
        announcementCard = view.findViewById(R.id.announcementCard);
        ivAnnouncementIcon = view.findViewById(R.id.ivAnnouncementIcon);
        tvAnnouncementTitle = view.findViewById(R.id.tvAnnouncementTitle);
        tvAnnouncementContent = view.findViewById(R.id.tvAnnouncementContent);
        btnCloseAnnouncement = view.findViewById(R.id.btnCloseAnnouncement);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 1;
            hasMore = true;
            loadPosts();
            loadAnnouncements();
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                searchQuery = etSearch.getText().toString().trim();
                currentPage = 1;
                hasMore = true;
                loadPosts();
                return true;
            }
            return false;
        });

        // 私信按钮点击
        view.findViewById(R.id.btnMessages).setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), com.schoolforum.app.ui.chat.ConversationsActivity.class));
            }
        });
        tvMessageBadge = view.findViewById(R.id.tvMessageBadge);

        // 通知按钮点击
        view.findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new Intent(getActivity(), com.schoolforum.app.ui.notification.NotificationsActivity.class));
            }
        });

        view.findViewById(R.id.btnUser).setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("show_profile", true);
                startActivity(intent);
            }
        });

        view.findViewById(R.id.fabPost).setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), EditPostActivity.class);
                startActivity(intent);
            }
        });

        User user = UserManager.getInstance(requireContext()).getCurrentUser();
        if (user != null && user.getAvatar() != null) {
            String avatarUrl = user.getAvatar().startsWith("http") ? user.getAvatar() 
                    : ApiClient.getBaseUrl() + user.getAvatar();
            com.bumptech.glide.Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(ivAvatar);
        }
        
        ivAvatar.setOnClickListener(v -> {
            User currentUser = UserManager.getInstance(requireContext()).getCurrentUser();
            if (currentUser != null && currentUser.getId() != null) {
                Intent intent = new Intent(getActivity(), com.schoolforum.app.ui.profile.ProfileActivity.class);
                intent.putExtra("userId", currentUser.getId());
                startActivity(intent);
            }
        });

        // 公告关闭按钮
        btnCloseAnnouncement.setOnClickListener(v -> {
            announcementBanner.setVisibility(View.GONE);
        });
    }

    private void setupRecyclerView() {
        adapter = new PostsAdapter();
        adapter.setOnPostClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new SpacesItemDecoration(8, false));
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

    private void loadAnnouncements() {
        ApiClient.getInstance(requireContext()).get("/announcements/active", null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    requireActivity().runOnUiThread(() -> {
                        try {
                            android.util.Log.d("PostsFragment", "公告API响应: " + responseBody);
                            Type type = new TypeToken<AnnouncementsResponse>(){}.getType();
                            AnnouncementsResponse response = gson.fromJson(responseBody, type);
                            android.util.Log.d("PostsFragment", "解析结果: success=" + response.success + ", announcements=" + (response.announcements != null ? response.announcements.size() : 0));
                            if (response.success && response.announcements != null && !response.announcements.isEmpty()) {
                                // 显示第一个顶部公告
                                for (Announcement announcement : response.announcements) {
                                    android.util.Log.d("PostsFragment", "公告位置: " + announcement.getDisplayPosition());
                                    if ("top".equals(announcement.getDisplayPosition())) {
                                        showAnnouncementBanner(announcement);
                                        break;
                                    }
                                }
                            } else {
                                android.util.Log.d("PostsFragment", "没有公告数据或 success=false");
                            }
                        } catch (Exception e) {
                            android.util.Log.e("PostsFragment", "解析公告失败: " + e.getMessage());
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("PostsFragment", "加载公告失败: " + error);
                }
            }, null);
    }

    private void showAnnouncementBanner(Announcement announcement) {
        if (announcementBanner == null || tvAnnouncementTitle == null || tvAnnouncementContent == null) {
            android.util.Log.e("PostsFragment", "公告组件未初始化");
            return;
        }
        
        currentAnnouncement = announcement;
        tvAnnouncementTitle.setText(announcement.getTitle());
        
        String content = announcement.getContent();
        if (content != null && content.length() > 100) {
            content = content.substring(0, 100) + "...";
        }
        tvAnnouncementContent.setText(content != null ? content : "");
        
        // 根据类型设置颜色
        int bgColor;
        int iconColor;
        String type = announcement.getType();
        if (type == null) type = "info";
        
        switch (type) {
            case "success":
                bgColor = getResources().getColor(R.color.announcement_success_bg, null);
                iconColor = getResources().getColor(R.color.announcement_success, null);
                break;
            case "warning":
                bgColor = getResources().getColor(R.color.announcement_warning_bg, null);
                iconColor = getResources().getColor(R.color.announcement_warning, null);
                break;
            case "danger":
                bgColor = getResources().getColor(R.color.announcement_danger_bg, null);
                iconColor = getResources().getColor(R.color.announcement_danger, null);
                break;
            default:
                bgColor = getResources().getColor(R.color.announcement_info_bg, null);
                iconColor = getResources().getColor(R.color.announcement_info, null);
                break;
        }
        
        if (announcementCard != null) {
            announcementCard.setCardBackgroundColor(bgColor);
        }
        if (ivAnnouncementIcon != null) {
            ivAnnouncementIcon.setColorFilter(iconColor);
        }
        tvAnnouncementTitle.setTextColor(iconColor);
        
        announcementBanner.setVisibility(View.VISIBLE);
        android.util.Log.d("PostsFragment", "公告横幅已显示: " + announcement.getTitle());
    }

    private void loadPosts() {
        if (isLoading) return;
        
        isLoading = true;
        if (currentPage == 1) {
            showLoading(true);
        }

        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(currentPage));
        params.put("limit", "20");
        if (!TextUtils.isEmpty(searchQuery)) {
            params.put("search", searchQuery);
        }
        params.put("sortBy", sortBy);

        ApiClient.getInstance(requireContext()).get("/posts", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    requireActivity().runOnUiThread(() -> {
                        isLoading = false;
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);

                        try {
                            PostsResponse response = gson.fromJson(responseBody, PostsResponse.class);
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
                    requireActivity().runOnUiThread(() -> {
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
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
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
        String userId = UserManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) {
            showError("请先登录");
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("userId", userId);

        ApiClient.getInstance(requireContext()).postForm("/posts/" + post.getId() + "/like", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    requireActivity().runOnUiThread(() -> {
                        try {
                            LikeResponse data = gson.fromJson(responseBody, LikeResponse.class);
                            if (data.success) {
                                post.setLikes(data.likes);
                                post.setLiked(data.liked);
                                post.setDislikes(data.dislikes);
                                post.setDisliked(data.disliked);
                                adapter.notifyItemChanged(position);
                            }
                        } catch (Exception e) {
                            showError("操作失败");
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    requireActivity().runOnUiThread(() -> showError(error));
                }
            }, null);
    }

    @Override
    public void onMoreClick(Post post, View anchor) {
        showPostMenu(post, anchor);
    }
    
    private void showPostMenu(Post post, View anchor) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(anchor.getContext(), anchor);
        
        String currentUserId = UserManager.getInstance(requireContext()).getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(post.getUserId());
        
        if (isOwner) {
            popup.getMenu().add(0, 1, 0, "删除");
        } else {
            popup.getMenu().add(0, 2, 0, "举报");
        }
        
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                // 删除帖子
                showDeleteConfirmDialog(post);
                return true;
            } else if (item.getItemId() == 2) {
                // 举报帖子
                showReportDialog(post);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void showDeleteConfirmDialog(Post post) {
        if (getActivity() == null) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除帖子")
            .setMessage("确定要删除这篇帖子吗？")
            .setPositiveButton("删除", (dialog, which) -> deletePost(post))
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void deletePost(Post post) {
        String currentUserId = UserManager.getInstance(requireContext()).getCurrentUserId();
        if (currentUserId == null) return;
        
        java.util.Map<String, String> params = new java.util.HashMap<>();
        params.put("postId", post.getId());
        params.put("userId", currentUserId);
        
        ApiClient.getInstance(requireContext()).postForm("/posts/delete", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            android.widget.Toast.makeText(requireContext(), "帖子已删除", android.widget.Toast.LENGTH_SHORT).show();
                            // 刷新列表
                            currentPage = 1;
                            hasMore = true;
                            loadPosts();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            android.widget.Toast.makeText(requireContext(), error, android.widget.Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            }, null);
    }
    
    private void showReportDialog(Post post) {
        startActivity(com.schoolforum.app.ui.report.ReportActivity.createIntent(
            requireContext(),
            com.schoolforum.app.ui.report.ReportActivity.TARGET_TYPE_POST,
            post.getId()
        ));
    }

    private static class PostsResponse {
        boolean success;
        String message;
        List<Post> posts;
        Pagination pagination;
    }

    private static class Pagination {
        int currentPage;
        int totalPages;
        int totalPosts;
        boolean hasNext;
        boolean hasPrev;
    }

    private static class LikeResponse {
        boolean success;
        int likes;
        boolean liked;
        int dislikes;
        boolean disliked;
    }

    private static class AnnouncementsResponse {
        boolean success;
        String message;
        List<Announcement> announcements;
    }
}
