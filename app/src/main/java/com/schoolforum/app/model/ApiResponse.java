package com.schoolforum.app.model;

import com.google.gson.annotations.SerializedName;

/**
 * API通用响应模型
 */
@SuppressWarnings("unused")
public class ApiResponse<T> {
    @SerializedName("success")
    private Boolean success;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("data")
    private T data;

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
