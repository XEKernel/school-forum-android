package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * 用户数据模型
 */
@SuppressWarnings("unused")
public class User {
    @SerializedName("id")
    private String id;
    
    @SerializedName("qq")
    private String qq;
    
    @SerializedName("username")
    private String username;
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("school")
    private String school;
    
    @SerializedName("grade")
    private String grade;
    
    @SerializedName("className")
    private String className;
    
    @SerializedName("avatar")
    private String avatar;
    
    @SerializedName("bio")
    private String bio;
    
    @SerializedName("birthday")
    private String birthday;
    
    @SerializedName("gender")
    private String gender;
    
    @SerializedName("enrollmentYear")
    private Integer enrollmentYear;
    
    @SerializedName("postCount")
    private Integer postCount;
    
    @SerializedName("commentCount")
    private Integer commentCount;
    
    @SerializedName("isActive")
    private Boolean isActive;
    
    @SerializedName("isAdmin")
    private Boolean isAdmin;
    
    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("lastLogin")
    private String lastLogin;
    
    @SerializedName("settings")
    private com.google.gson.JsonObject settings;
    
    @SerializedName("isFollowing")
    private Boolean isFollowing;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getQq() { return qq; }
    public void setQq(String qq) { this.qq = qq; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public Integer getEnrollmentYear() { return enrollmentYear; }
    public void setEnrollmentYear(Integer enrollmentYear) { this.enrollmentYear = enrollmentYear; }
    
    public Integer getPostCount() { return postCount; }
    public void setPostCount(Integer postCount) { this.postCount = postCount; }
    
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
    
    public com.google.gson.JsonObject getSettings() { return settings; }
    public void setSettings(com.google.gson.JsonObject settings) { this.settings = settings; }
    
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean isFollowing) { this.isFollowing = isFollowing; }
}
