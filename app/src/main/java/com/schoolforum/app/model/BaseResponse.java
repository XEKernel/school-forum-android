package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * API通用基础响应模型
 * 供各页面共享使用，避免重复定义
 */
public class BaseResponse {
    @SerializedName("success")
    public boolean success;
    
    @SerializedName("message")
    public String message;
}
