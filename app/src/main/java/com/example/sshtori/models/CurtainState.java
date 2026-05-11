package com.example.sshtori.models;

import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Модель состояния штор
 * Адаптировано для прямого управления ESP32
 */
public class CurtainState {

    // Статусы, соответствующие прошивке ESP32
    public enum Status {
        OPENED("OPENED"),
        CLOSED("CLOSED"),
        MOVING("MOVING"),
        STOPPED("STOPPED"),
        CALIBRATING("CALIBRATING"),
        UNKNOWN("UNKNOWN"),
        ERROR("ERROR");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Status fromString(String text) {
            if (text == null) return UNKNOWN;
            for (Status s : Status.values()) {
                if (s.value.equalsIgnoreCase(text.trim())) {
                    return s;
                }
            }
            return UNKNOWN;
        }
    }

    // Поля
    private String deviceId;
    private String deviceName;
    private Status status;
    private int position;
    private boolean isMoving;
    private long lastUpdate;
    private String errorMessage;
    private int targetPosition;
    private long movingStartTime;

    // Конструкторы
    public CurtainState() {
        this.deviceId = "curtain_1";
        this.deviceName = "Умные шторы";
        this.status = Status.UNKNOWN;
        this.position = 0;
        this.isMoving = false;
        this.lastUpdate = System.currentTimeMillis();
        this.errorMessage = null;
        this.targetPosition = -1;
        this.movingStartTime = 0;
    }

    public CurtainState(String deviceId, String deviceName, Status status, int position, boolean isMoving) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.status = status;
        this.position = Math.max(0, Math.min(100, position));
        this.isMoving = isMoving;
        this.lastUpdate = System.currentTimeMillis();
        this.errorMessage = null;
        this.targetPosition = -1;
        this.movingStartTime = isMoving ? System.currentTimeMillis() : 0;
    }

    /**
     * Парсинг ответа от ESP32
     */
    public static CurtainState parseFromEsp32Response(String response) {
        CurtainState state = new CurtainState();

        if (response == null || response.isEmpty()) {
            state.status = Status.ERROR;
            state.errorMessage = "Пустой ответ от устройства";
            return state;
        }

        try {
            if (response.trim().startsWith("{")) {
                JSONObject json = new JSONObject(response);

                if (json.has("status")) {
                    state.status = Status.fromString(json.getString("status"));
                }
                if (json.has("position")) {
                    state.position = json.getInt("position");
                }
                if (json.has("is_moving")) {
                    state.isMoving = json.getBoolean("is_moving");
                }
                if (json.has("target_position")) {
                    state.targetPosition = json.getInt("target_position");
                }
                if (json.has("device_id")) {
                    state.deviceId = json.getString("device_id");
                }
                if (json.has("device_name")) {
                    state.deviceName = json.getString("device_name");
                }
            } else {
                String trimmedResponse = response.trim().toUpperCase();
                state.status = Status.fromString(trimmedResponse);

                switch (state.status) {
                    case OPENED:
                        state.position = 100;
                        state.isMoving = false;
                        break;
                    case CLOSED:
                        state.position = 0;
                        state.isMoving = false;
                        break;
                    case MOVING:
                        state.isMoving = true;
                        break;
                    case STOPPED:
                        state.isMoving = false;
                        break;
                    case ERROR:
                        state.errorMessage = response;
                        break;
                }

                if (response.contains("%")) {
                    try {
                        String percentage = response.replaceAll("[^0-9]", "");
                        if (!percentage.isEmpty()) {
                            state.position = Integer.parseInt(percentage);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (JSONException e) {
            state.status = Status.ERROR;
            state.errorMessage = "Ошибка парсинга: " + e.getMessage();
        }

        state.lastUpdate = System.currentTimeMillis();
        return state;
    }

    /**
     * Возвращает отформатированный статус на русском языке
     */
    public String getFormattedStatus() {
        switch (status) {
            case OPENED:
                return "Открыто";
            case CLOSED:
                return "Закрыто";
            case MOVING:
                return "Движется...";
            case STOPPED:
                return "Остановлено";
            case CALIBRATING:
                return "Калибровка...";
            case ERROR:
                return "Ошибка: " + (errorMessage != null ? errorMessage : "Неизвестно");
            default:
                return "Неизвестно";
        }
    }

    /**
     * Создает JSON для сохранения состояния
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("device_id", deviceId);
            json.put("device_name", deviceName);
            json.put("status", status.getValue());
            json.put("position", position);
            json.put("is_moving", isMoving);
            json.put("last_update", lastUpdate);
            if (targetPosition != -1) {
                json.put("target_position", targetPosition);
            }
            if (errorMessage != null) {
                json.put("error_message", errorMessage);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    // Getters
    public String getDeviceId() { return deviceId; }
    public String getDeviceName() { return deviceName; }
    public Status getStatus() { return status; }
    public int getPosition() { return position; }
    public boolean isMoving() { return isMoving; }
    public long getLastUpdate() { return lastUpdate; }
    public String getErrorMessage() { return errorMessage; }
    public int getTargetPosition() { return targetPosition; }
    public long getMovingStartTime() { return movingStartTime; }

    // Setters
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public void setStatus(Status status) {
        this.status = status;
        updateMovingFlag();
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setPosition(int position) {
        this.position = Math.max(0, Math.min(100, position));
        this.lastUpdate = System.currentTimeMillis();
        if (position == 0 && status != Status.MOVING) {
            this.status = Status.CLOSED;
            this.isMoving = false;
        } else if (position == 100 && status != Status.MOVING) {
            this.status = Status.OPENED;
            this.isMoving = false;
        }
    }

    public void setMoving(boolean moving) {
        this.isMoving = moving;
        if (moving) {
            this.movingStartTime = System.currentTimeMillis();
        } else {
            this.movingStartTime = 0;
        }
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        if (errorMessage != null) {
            this.status = Status.ERROR;
        }
    }

    public void setTargetPosition(int targetPosition) {
        this.targetPosition = targetPosition;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    // Вспомогательные методы
    private void updateMovingFlag() {
        this.isMoving = (status == Status.MOVING);
        if (isMoving) {
            this.movingStartTime = System.currentTimeMillis();
        } else {
            this.movingStartTime = 0;
        }
    }

    public String getStatusInRussian() {
        return getFormattedStatus();
    }


    public int getPositionPercentage() {
        return position;
    }

    public boolean isOpened() {
        return status == Status.OPENED || (position == 100 && !isMoving);
    }

    public boolean isClosed() {
        return status == Status.CLOSED || (position == 0 && !isMoving);
    }

    public boolean hasError() {
        return status == Status.ERROR;
    }

    public long getElapsedMovingTime() {
        if (!isMoving || movingStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - movingStartTime;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("CurtainState{id='%s', name='%s', status=%s, position=%d%%, isMoving=%b, lastUpdate=%d}",
                deviceId, deviceName, status.getValue(), position, isMoving, lastUpdate);
    }
}