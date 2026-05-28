package com.socialpersona.gateway.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ImageGenerateResponse {

    private boolean success;
    @JsonProperty("local_path")
    private String localPath;
    private int width;
    private int height;
    private String error;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
