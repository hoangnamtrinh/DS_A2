package main.helpers;

import org.json.JSONObject;

public class GETClientDataResponse {
    private final boolean isError;
    private final String errorMessage;
    private final JSONObject data;

    private GETClientDataResponse(boolean isError, String errorMessage, JSONObject data) {
        this.isError = isError;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public static GETClientDataResponse error(String errorMessage) {
        return new GETClientDataResponse(true, errorMessage, null);
    }

    public static GETClientDataResponse success(JSONObject data) {
        return new GETClientDataResponse(false, null, data);
    }

    public boolean isError() {
        return isError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JSONObject getData() {
        return data;
    }
}

