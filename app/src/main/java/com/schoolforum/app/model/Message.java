package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * 私信消息数据模型
 */
@SuppressWarnings("unused")
public class Message {
    @SerializedName("id")
    private String id;
    
    @SerializedName("conversationId")
    private String conversationId;
    
    @SerializedName("senderId")
    private String senderId;
    
    @SerializedName("receiverId")
    private String receiverId;
    
    @SerializedName("content")
    private String content;
    
    @SerializedName("type")
    private String type; // text, image, etc.
    
    @SerializedName("read")
    private Boolean read;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("senderUsername")
    private String senderUsername;
    
    @SerializedName("senderAvatar")
    private String senderAvatar;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    
    public String getSenderAvatar() { return senderAvatar; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
}
