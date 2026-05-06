package com.example.sshtori.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Модель ответа от сервера
 */
public class ServerResponse {
    private boolean success;
    private String message;
    private CurtainState curtainState;
    private String error;

    public ServerResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public CurtainState getCurtainState() {
        return curtainState;
    }

    public String getError() {
        return error;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCurtainState(CurtainState curtainState) {
        this.curtainState = curtainState;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * Создание ответа из JSON
     */
    public static ServerResponse fromJSON(JSONObject json) {
        try {
            boolean success = json.optBoolean("success", false);
            String message = json.optString("message", "");

            ServerResponse response = new ServerResponse(success, message);
            response.setError(json.optString("error", null));

            // Парсинг состояния штор, если есть
            if (json.has("curtain_state")) {
                JSONObject stateJson = json.getJSONObject("curtain_state");
                CurtainState state = new CurtainState();
                state.setId(stateJson.optString("id", ""));
                state.setName(stateJson.optString("name", ""));
                state.setPosition(stateJson.optInt("position", 0));
                state.setMoving(stateJson.optBoolean("is_moving", false));
                state.setStatus(stateJson.optString("status", "unknown"));
                state.setLastUpdate(stateJson.optLong("last_update", System.currentTimeMillis()));
                response.setCurtainState(state);
            }

            return response;
        } catch (JSONException e) {
            e.printStackTrace();
            return new ServerResponse(false, "Ошибка парсинга ответа");
        }
    }
}
