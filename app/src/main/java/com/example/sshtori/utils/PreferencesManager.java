package com.example.sshtori.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Менеджер для работы с настройками приложения
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "CurtainAppPrefs";

    // Ключи настроек
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_DEFAULT_CURTAIN_ID = "default_curtain_id";
    private static final String KEY_NODEMCU_IP = "nodemcu_ip";
    private static final String KEY_NODEMCU_PORT = "nodemcu_port";
    private static final String KEY_AUTO_UPDATE_INTERVAL = "auto_update_interval";
    private static final String KEY_CONNECTION_TIMEOUT = "connection_timeout";
    private static final String KEY_ENABLE_VIBRATION = "enable_vibration";
    private static final String KEY_ENABLE_SOUND = "enable_sound";
    private static final String KEY_CURTAIN_NAME = "curtain_name";

    // Значения по умолчанию
    private static final String DEFAULT_SERVER_URL = "http://192.168.1.100:5000";
    private static final String DEFAULT_CURTAIN_ID = "curtain_1";
    private static final String DEFAULT_CURTAIN_NAME = "Штора 1";
    private static final String DEFAULT_NODEMCU_IP = "192.168.1.101";
    private static final int DEFAULT_NODEMCU_PORT = 80;
    private static final int DEFAULT_AUTO_UPDATE_INTERVAL = 5000; // 5 секунд
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5 секунд
    private static final boolean DEFAULT_ENABLE_VIBRATION = true;
    private static final boolean DEFAULT_ENABLE_SOUND = false;

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ========== URL сервера ==========
    public String getServerUrl() {
        return preferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    public void setServerUrl(String url) {
        preferences.edit().putString(KEY_SERVER_URL, url).apply();
    }

    // ========== ID штор ==========
    public String getDefaultCurtainId() {
        return preferences.getString(KEY_DEFAULT_CURTAIN_ID, DEFAULT_CURTAIN_ID);
    }

    public void setDefaultCurtainId(String curtainId) {
        preferences.edit().putString(KEY_DEFAULT_CURTAIN_ID, curtainId).apply();
    }

    // ========== Название штор ==========
    public String getCurtainName() {
        return preferences.getString(KEY_CURTAIN_NAME, DEFAULT_CURTAIN_NAME);
    }

    public void setCurtainName(String name) {
        preferences.edit().putString(KEY_CURTAIN_NAME, name).apply();
    }

    // ========== IP адрес NodeMCU ==========
    public String getNodeMcuIp() {
        return preferences.getString(KEY_NODEMCU_IP, DEFAULT_NODEMCU_IP);
    }

    public void setNodeMcuIp(String ip) {
        preferences.edit().putString(KEY_NODEMCU_IP, ip).apply();
    }

    // ========== Порт NodeMCU ==========
    public int getNodeMcuPort() {
        return preferences.getInt(KEY_NODEMCU_PORT, DEFAULT_NODEMCU_PORT);
    }

    public void setNodeMcuPort(int port) {
        preferences.edit().putInt(KEY_NODEMCU_PORT, port).apply();
    }

    // ========== Интервал автообновления ==========
    public int getAutoUpdateInterval() {
        return preferences.getInt(KEY_AUTO_UPDATE_INTERVAL, DEFAULT_AUTO_UPDATE_INTERVAL);
    }

    public void setAutoUpdateInterval(int interval) {
        preferences.edit().putInt(KEY_AUTO_UPDATE_INTERVAL, interval).apply();
    }

    // ========== Таймаут соединения ==========
    public int getConnectionTimeout() {
        return preferences.getInt(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
    }

    public void setConnectionTimeout(int timeout) {
        preferences.edit().putInt(KEY_CONNECTION_TIMEOUT, timeout).apply();
    }

    // ========== Вибрация ==========
    public boolean isVibrationEnabled() {
        return preferences.getBoolean(KEY_ENABLE_VIBRATION, DEFAULT_ENABLE_VIBRATION);
    }

    public void setVibrationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLE_VIBRATION, enabled).apply();
    }

    // ========== Звук ==========
    public boolean isSoundEnabled() {
        return preferences.getBoolean(KEY_ENABLE_SOUND, DEFAULT_ENABLE_SOUND);
    }

    public void setSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLE_SOUND, enabled).apply();
    }

    // ========== Сброс настроек ==========
    public void resetToDefaults() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
        editor.putString(KEY_DEFAULT_CURTAIN_ID, DEFAULT_CURTAIN_ID);
        editor.putString(KEY_CURTAIN_NAME, DEFAULT_CURTAIN_NAME);
        editor.putString(KEY_NODEMCU_IP, DEFAULT_NODEMCU_IP);
        editor.putInt(KEY_NODEMCU_PORT, DEFAULT_NODEMCU_PORT);
        editor.putInt(KEY_AUTO_UPDATE_INTERVAL, DEFAULT_AUTO_UPDATE_INTERVAL);
        editor.putInt(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        editor.putBoolean(KEY_ENABLE_VIBRATION, DEFAULT_ENABLE_VIBRATION);
        editor.putBoolean(KEY_ENABLE_SOUND, DEFAULT_ENABLE_SOUND);
        editor.apply();
    }

    // ========== Экспорт всех настроек ==========
    public String exportSettings() {
        return "Server URL: " + getServerUrl() + "\n" +
               "Curtain ID: " + getDefaultCurtainId() + "\n" +
               "Curtain Name: " + getCurtainName() + "\n" +
               "NodeMCU IP: " + getNodeMcuIp() + "\n" +
               "NodeMCU Port: " + getNodeMcuPort() + "\n" +
               "Auto Update: " + getAutoUpdateInterval() + "ms\n" +
               "Timeout: " + getConnectionTimeout() + "ms\n" +
               "Vibration: " + isVibrationEnabled() + "\n" +
               "Sound: " + isSoundEnabled();
    }
}
