package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * 公告模型
 */
public class Announcement {
    @SerializedName("_id")
    private String id;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("type")
    private String type; // info, success, warning, danger
    
    @SerializedName("isActive")
    private boolean isActive;
    
    @SerializedName("isPinned")
    private boolean isPinned;
    
    @SerializedName("displayPosition")
    private String displayPosition; // top, popup, list
    
    @SerializedName("startTime")
    private String startTime;
    
    @SerializedName("endTime")
    private String endTime;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("viewCount")
    private int viewCount;
    
    @SerializedName("createdBy")
    private CreatedBy createdBy;
    
    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getType() { return type; }
    public boolean isActive() { return isActive; }
    public boolean isPinned() { return isPinned; }
    public String getDisplayPosition() { return displayPosition; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getCreatedAt() { return createdAt; }
    public int getViewCount() { return viewCount; }
    public CreatedBy getCreatedByObj() { return createdBy; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setType(String type) { this.type = type; }
    public void setActive(boolean active) { isActive = active; }
    public void setPinned(boolean pinned) { isPinned = pinned; }
    public void setDisplayPosition(String displayPosition) { this.displayPosition = displayPosition; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    public void setCreatedBy(CreatedBy createdBy) { this.createdBy = createdBy; }
    
    // 获取创建者用户名
    public String getCreatedBy() {
        return createdBy != null ? createdBy.getUsername() : "未知";
    }
    
    /**
     * 创建者信息
     */
    public static class CreatedBy {
        @SerializedName("username")
        private String username;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
