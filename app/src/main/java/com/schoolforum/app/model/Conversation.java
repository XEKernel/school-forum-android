package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * 会话数据模型
 */
@SuppressWarnings("unused")
public class Conversation {
    @SerializedName("id")
    private String id;
    
    @SerializedName("otherUser")
    private OtherUser otherUser;
    
    @SerializedName("lastMessage")
    private LastMessage lastMessage;
    
    @SerializedName("updatedAt")
    private String updatedAt;
    
    @SerializedName("unreadCount")
    private Integer unreadCount;

    /**
     * 对方用户信息
     */
    public static class OtherUser {
        @SerializedName("id")
        private String id;
        
        @SerializedName("username")
        private String username;
        
        @SerializedName("avatar")
        private String avatar;
        
        @SerializedName("school")
        private String school;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        
        public String getSchool() { return school; }
        public void setSchool(String school) { this.school = school; }
    }

    /**
     * 最后一条消息
     */
    public static class LastMessage {
        @SerializedName("content")
        private String content;
        
        @SerializedName("senderId")
        private String senderId;
        
        @SerializedName("createdAt")
        private String createdAt;

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public OtherUser getOtherUser() { return otherUser; }
    public void setOtherUser(OtherUser otherUser) { this.otherUser = otherUser; }
    
    public LastMessage getLastMessage() { return lastMessage; }
    public void setLastMessage(LastMessage lastMessage) { this.lastMessage = lastMessage; }
    
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    
    public Integer getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }
}
