package com.microvault.auth.dto;

public class ApiDataResponse {

    private boolean ok;
    private Object data;
    private String message;

    public static ApiDataResponse of(Object data) {
        ApiDataResponse response = new ApiDataResponse();
        response.ok = true;
        response.data = data;
        return response;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
