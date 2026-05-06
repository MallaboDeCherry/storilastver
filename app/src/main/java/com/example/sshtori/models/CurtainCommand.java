package com.example.sshtori.models;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Модель команды для управления шторами
 */
public class CurtainCommand {
    private String curtainId;
    private String action; // "open", "close", "stop", "set_position"
    private int position; // 0-100
    private long timestamp;

    public CurtainCommand(String curtainId, String action) {
        this.curtainId = curtainId;
        this.action = action;
        this.position = -1;
        this.timestamp = System.currentTimeMillis();
    }

    public CurtainCommand(String curtainId, String action, int position) {
        this.curtainId = curtainId;
        this.action = action;
        this.position = position;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getCurtainId() {
        return curtainId;
    }

    public String getAction() {
        return action;
    }

    public int getPosition() {
        return position;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setCurtainId(String curtainId) {
        this.curtainId = curtainId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Преобразование команды в JSON
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("curtain_id", curtainId);
            json.put("action", action);
            if (position >= 0) {
                json.put("position", position);
            }
            json.put("timestamp", timestamp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Создание команды из JSON
     */
    public static CurtainCommand fromJSON(JSONObject json) {
        try {
            String curtainId = json.getString("curtain_id");
            String action = json.getString("action");
            int position = json.optInt("position", -1);

            CurtainCommand command = new CurtainCommand(curtainId, action, position);
            command.setTimestamp(json.optLong("timestamp", System.currentTimeMillis()));
            return command;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
