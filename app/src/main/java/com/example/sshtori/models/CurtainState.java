package com.example.sshtori.models;

/**
 * Модель состояния штор
 */
public class CurtainState {
    private String id;
    private String name;
    private int position; // 0-100 (0 = закрыто, 100 = открыто)
    private boolean isMoving;
    private String status; // "open", "closed", "moving", "stopped"
    private long lastUpdate;

    public CurtainState() {
        this.position = 0;
        this.isMoving = false;
        this.status = "closed";
        this.lastUpdate = System.currentTimeMillis();
    }

    public CurtainState(String id, String name, int position, boolean isMoving, String status) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.isMoving = isMoving;
        this.status = status;
        this.lastUpdate = System.currentTimeMillis();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public String getStatus() {
        return status;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(int position) {
        this.position = Math.max(0, Math.min(100, position));
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setStatus(String status) {
        this.status = status;
        this.lastUpdate = System.currentTimeMillis();
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getStatusInRussian() {
        switch (status) {
            case "open":
                return "Открыто";
            case "closed":
                return "Закрыто";
            case "moving":
                return "Движение";
            case "stopped":
                return "Остановлено";
            default:
                return "Неизвестно";
        }
    }
}
