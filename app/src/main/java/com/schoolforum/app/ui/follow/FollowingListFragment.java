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
import com.google.gson.reflect.TypeToken;
import com.schoolforum.app.R;
import com.schoolforum.app.model.User;
import com.schoolforum.app.network.ApiClient;
import com.schoolforum.app.ui.profile.ProfileActivity;
import com.schoolforum.app.utils.UserManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 关注列表Fragment
 */
public class FollowingListFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyView;

    private FollowingUserAdapter adapter;
    private List<User> users;
    private final Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_following_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        users = new ArrayList<>();
        initViews(view);
        setupRecyclerView();
        loadFollowingList();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadFollowingList);
    }

    private void setupRecyclerView() {
        adapter = new FollowingUserAdapter();
        adapter.setOnUserActionListener(new FollowingUserAdapter.OnUserActionListener() {
            @Override
            public void onUserClick(User user) {
                openUserProfile(user.getId());
            }

            @Override
            public void onUnfollowClick(User user, int position) {
                unfollowUser(user, position);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadFollowingList() {
        String userId = UserManager.getInstance(requireContext()).getCurrentUserId();
        if (userId == null) return;

        showLoading(true);

        ApiClient.getInstance(requireContext()).get("/following/" + userId, null,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String responseBody) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);

                        try {
                            Type type = new TypeToken<FollowingListResponse>(){}.getType();
                            FollowingListResponse response = gson.fromJson(responseBody, type);
                            if (response.success) {
                                users.clear();
                                if (response.following != null) {
                                    users.addAll(response.following);
                                }
                                adapter.setUsers(new ArrayList<>(users));
                                emptyView.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
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
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        showError(error);
                    });
                }
            }, null);
    }

    private void unfollowUser(User user, int position) {
        if (user == null || user.getId() == null) return;
        String currentUserId = UserManager.getInstance(requireContext()).getCurrentUserId();
        if (currentUserId == null) return;

        Map<String, String> params = new HashMap<>();
        // followerId 由服务端从 JWT 取身份（此处传 followingId 即可）
        params.put("followingId", user.getId());

        ApiClient.getInstance(requireContext()).postForm("/unfollow", params,
            new ApiClient.ApiCallback<String>() {
                @Override
                public void onSuccess(String data) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        // 按用户 ID 查找删除，避免异步期间 position 失效删错行
                        int idx = -1;
                        for (int i = 0; i < users.size(); i++) {
                            if (users.get(i) != null && user.getId().equals(users.get(i).getId())) {
                                idx = i;
                                break;
                            }
                        }
                        if (idx >= 0) {
                            users.remove(idx);
                        }
                        adapter.setUsers(new ArrayList<>(users));
                        emptyView.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                        if (getContext() != null) {
                            android.widget.Toast.makeText(getContext(), "已取消关注", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> showError(error));
                }
            }, null);
    }

    private void openUserProfile(String userId) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), ProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        if (getView() != null && getContext() != null) {
            android.widget.Toast.makeText(getContext(), message, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private static class FollowingListResponse {
        boolean success;
        String message;
        List<User> following;
    }
}
