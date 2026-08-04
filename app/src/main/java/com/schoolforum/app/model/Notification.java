package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * 通知数据模型
 */
@SuppressWarnings("unused")
public class Notification {
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("type")
    private String type; // like, comment, comment_reply, follow, system
    
    @SerializedName("postId")
    private String postId;
    
    @SerializedName("postTitle")
    private String postTitle;
    
    @SerializedName("fromUserId")
    private String fromUserId;
    
    @SerializedName("fromUsername")
    private String fromUsername;
    
    @SerializedName("fromUserAvatar")
    private String fromUserAvatar;
    
    @SerializedName("timestamp")
    private String timestamp;
    
    @SerializedName("read")
    private Boolean read;
    
    @SerializedName("postExists")
    private Boolean postExists;
    
    @SerializedName("content")
    private String content;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    
    public String getPostTitle() { return postTitle; }
    public void setPostTitle(String postTitle) { this.postTitle = postTitle; }
    
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    
    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }
    
    public String getFromUserAvatar() { return fromUserAvatar; }
    public void setFromUserAvatar(String fromUserAvatar) { this.fromUserAvatar = fromUserAvatar; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    
    public Boolean getPostExists() { return postExists; }
    public void setPostExists(Boolean postExists) { this.postExists = postExists; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    /**
     * 获取通知显示文本
     */
    public String getDisplayText() {
        if (type == null) return "";
        
        switch (type) {
            case "like":
                return fromUsername + " 赞了你的帖子";
            case "comment":
                return fromUsername + " 评论了你的帖子";
            case "comment_reply":
                return fromUsername + " 回复了你的评论";
            case "follow":
                return fromUsername + " 关注了你";
            case "system":
                return content != null ? content : "系统通知";
            default:
                return fromUsername + " 与你互动";
        }
    }
    
    /**
     * 获取通知图标类型
     */
    public int getIconType() {
        if (type == null) return 0;
        
        switch (type) {
            case "like":
                return 1; // 心形图标
            case "comment":
            case "comment_reply":
                return 2; // 评论图标
            case "follow":
                return 3; // 关注图标
            case "system":
                return 4; // 系统图标
            default:
                return 0;
        }
    }
}
