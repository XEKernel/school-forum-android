package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 帖子数据模型
 */
@SuppressWarnings("unused")
public class Post {
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("userAvatar")
    private String userAvatar;
    
    @SerializedName("school")
    private String school;
    
    @SerializedName("grade")
    private String grade;
    
    @SerializedName("className")
    private String className;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("images")
    private List<Image> images;
    
    @SerializedName("anonymous")
    private Boolean anonymous;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("timestamp")
    private String timestamp;
    
    @SerializedName("likes")
    private Integer likes;
    
    @SerializedName("dislikes")
    private Integer dislikes;
    
    @SerializedName("viewCount")
    private Integer viewCount;
    
    @SerializedName("comments")
    private List<Comment> comments;
    
    @SerializedName("isDeleted")
    private Boolean isDeleted;
    
    @SerializedName("liked")
    private Boolean liked;
    
    @SerializedName("disliked")
    private Boolean disliked;
    
    @SerializedName("favorited")
    private Boolean favorited;
    
    @SerializedName("deviceInfo")
    private String deviceInfo;

    /**
     * 图片模型
     */
    public static class Image {
        @SerializedName("url")
        private String url;
        
        @SerializedName("filename")
        private String filename;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
    }

    /**
     * 评论模型
     */
    public static class Comment {
        @SerializedName("id")
        private String id;
        
        @SerializedName("userId")
        private String userId;
        
        @SerializedName("username")
        private String username;
        
        @SerializedName("userAvatar")
        private String userAvatar;
        
        @SerializedName("content")
        private String content;
        
        @SerializedName("anonymous")
        private Boolean anonymous;
        
        @SerializedName("timestamp")
        private String timestamp;
        
        @SerializedName("replies")
        private List<Reply> replies;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getUserAvatar() { return userAvatar; }
        public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Boolean getAnonymous() { return anonymous; }
        public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public List<Reply> getReplies() { return replies; }
        public void setReplies(List<Reply> replies) { this.replies = replies; }
    }

    /**
     * 回复模型（支持嵌套）
     */
    public static class Reply {
        @SerializedName("id")
        private String id;
        
        @SerializedName("userId")
        private String userId;
        
        @SerializedName("username")
        private String username;
        
        @SerializedName("userAvatar")
        private String userAvatar;
        
        @SerializedName("content")
        private String content;
        
        @SerializedName("anonymous")
        private Boolean anonymous;
        
        @SerializedName("timestamp")
        private String timestamp;
        
        @SerializedName("replyTo")
        private String replyTo;
        
        @SerializedName("replyToId")
        private String replyToId;
        
        @SerializedName("replyToUsername")
        private String replyToUsername;
        
        @SerializedName("replies")
        private List<Reply> replies;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getUserAvatar() { return userAvatar; }
        public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Boolean getAnonymous() { return anonymous; }
        public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getReplyTo() { return replyTo; }
        public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
        
        public String getReplyToId() { return replyToId; }
        public void setReplyToId(String replyToId) { this.replyToId = replyToId; }
        
        public String getReplyToUsername() { return replyToUsername; }
        public void setReplyToUsername(String replyToUsername) { this.replyToUsername = replyToUsername; }
        
        public List<Reply> getReplies() { return replies; }
        public void setReplies(List<Reply> replies) { this.replies = replies; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public List<Image> getImages() { return images; }
    public void setImages(List<Image> images) { this.images = images; }
    
    public Boolean getAnonymous() { return anonymous; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public Integer getLikes() { return likes; }
    public void setLikes(Integer likes) { this.likes = likes; }
    
    public Integer getDislikes() { return dislikes; }
    public void setDislikes(Integer dislikes) { this.dislikes = dislikes; }
    
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    
    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    
    public String getDeviceInfo() { return deviceInfo; }
    public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    
    public Boolean getLiked() { return liked; }
    public void setLiked(Boolean liked) { this.liked = liked; }
    
    public Boolean getDisliked() { return disliked; }
    public void setDisliked(Boolean disliked) { this.disliked = disliked; }
    
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
}
