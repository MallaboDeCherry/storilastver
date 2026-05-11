package com.example.sshtori.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Менеджер для работы с настройками приложения
 * Версия 2.0 - для прямого управления ESP32
 */
public class PreferencesManager {
    private static final String PREFS_NAME = "CurtainAppPrefs";

    // Ключи настроек ESP32
    private static final String KEY_ESP32_IP = "esp32_ip";
    private static final String KEY_ESP32_PORT = "esp32_port";
    private static final String KEY_USE_MDNS = "use_mdns";
    private static final String KEY_MDNS_HOSTNAME = "mdns_hostname";

    // Ключи настроек штор
    private static final String KEY_CURTAIN_ID = "curtain_id";
    private static final String KEY_CURTAIN_NAME = "curtain_name";

    // Ключи настроек приложения
    private static final String KEY_AUTO_UPDATE_INTERVAL = "auto_update_interval";
    private static final String KEY_CONNECTION_TIMEOUT = "connection_timeout";
    private static final String KEY_ENABLE_VIBRATION = "enable_vibration";
    private static final String KEY_ENABLE_SOUND = "enable_sound";
    private static final String KEY_SHOW_CONNECTION_ERRORS = "show_connection_errors";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    // ============= НОВЫЕ КЛЮЧИ ДЛЯ АВТОРИЗАЦИИ =============
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_LOGIN_TYPE = "login_type"; // "registered" или "guest"

    // Значения по умолчанию для ESP32
    private static final String DEFAULT_ESP32_IP = "192.168.1.100";
    private static final int DEFAULT_ESP32_PORT = 80;
    private static final boolean DEFAULT_USE_MDNS = true;
    private static final String DEFAULT_MDNS_HOSTNAME = "smart-curtain.local";

    // Значения по умолчанию для штор
    private static final String DEFAULT_CURTAIN_ID = "curtain_1";
    private static final String DEFAULT_CURTAIN_NAME = "Умные шторы";

    // Значения по умолчанию для приложения
    private static final int DEFAULT_AUTO_UPDATE_INTERVAL = 2000;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 3000;
    private static final boolean DEFAULT_ENABLE_VIBRATION = true;
    private static final boolean DEFAULT_ENABLE_SOUND = false;
    private static final boolean DEFAULT_SHOW_CONNECTION_ERRORS = true;

    private final SharedPreferences preferences;

    public PreferencesManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ========== ESP32 НАСТРОЙКИ ==========

    public String getEsp32Ip() {
        return preferences.getString(KEY_ESP32_IP, DEFAULT_ESP32_IP);
    }

    public void setEsp32Ip(String ip) {
        preferences.edit().putString(KEY_ESP32_IP, ip).apply();
    }

    public int getEsp32Port() {
        return preferences.getInt(KEY_ESP32_PORT, DEFAULT_ESP32_PORT);
    }

    public void setEsp32Port(int port) {
        preferences.edit().putInt(KEY_ESP32_PORT, port).apply();
    }

    public boolean isUseMdns() {
        return preferences.getBoolean(KEY_USE_MDNS, DEFAULT_USE_MDNS);
    }

    public void setUseMdns(boolean use) {
        preferences.edit().putBoolean(KEY_USE_MDNS, use).apply();
    }

    public String getMdnsHostname() {
        return preferences.getString(KEY_MDNS_HOSTNAME, DEFAULT_MDNS_HOSTNAME);
    }

    public void setMdnsHostname(String hostname) {
        preferences.edit().putString(KEY_MDNS_HOSTNAME, hostname).apply();
    }

    // ========== НАСТРОЙКИ ШТОР ==========

    public String getDefaultCurtainId() {
        return preferences.getString(KEY_CURTAIN_ID, DEFAULT_CURTAIN_ID);
    }

    public void setDefaultCurtainId(String curtainId) {
        preferences.edit().putString(KEY_CURTAIN_ID, curtainId).apply();
    }

    public String getCurtainName() {
        return preferences.getString(KEY_CURTAIN_NAME, DEFAULT_CURTAIN_NAME);
    }

    public void setCurtainName(String name) {
        preferences.edit().putString(KEY_CURTAIN_NAME, name).apply();
    }

    // ========== НАСТРОЙКИ ПРИЛОЖЕНИЯ ==========

    public int getAutoUpdateInterval() {
        return preferences.getInt(KEY_AUTO_UPDATE_INTERVAL, DEFAULT_AUTO_UPDATE_INTERVAL);
    }

    public void setAutoUpdateInterval(int interval) {
        preferences.edit().putInt(KEY_AUTO_UPDATE_INTERVAL, interval).apply();
    }

    public int getConnectionTimeout() {
        return preferences.getInt(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
    }

    public void setConnectionTimeout(int timeout) {
        preferences.edit().putInt(KEY_CONNECTION_TIMEOUT, timeout).apply();
    }

    public boolean isVibrationEnabled() {
        return preferences.getBoolean(KEY_ENABLE_VIBRATION, DEFAULT_ENABLE_VIBRATION);
    }

    public void setVibrationEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLE_VIBRATION, enabled).apply();
    }

    public boolean isSoundEnabled() {
        return preferences.getBoolean(KEY_ENABLE_SOUND, DEFAULT_ENABLE_SOUND);
    }

    public void setSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLE_SOUND, enabled).apply();
    }

    public boolean isShowConnectionErrors() {
        return preferences.getBoolean(KEY_SHOW_CONNECTION_ERRORS, DEFAULT_SHOW_CONNECTION_ERRORS);
    }

    public void setShowConnectionErrors(boolean show) {
        preferences.edit().putBoolean(KEY_SHOW_CONNECTION_ERRORS, show).apply();
    }

    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean firstLaunch) {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, firstLaunch).apply();
    }

    // ========== НОВЫЕ МЕТОДЫ ДЛЯ АВТОРИЗАЦИИ ==========

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public String getUserEmail() {
        return preferences.getString(KEY_USER_EMAIL, "");
    }

    public void setUserEmail(String email) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply();
    }

    public String getLoginType() {
        return preferences.getString(KEY_LOGIN_TYPE, "guest");
    }

    public void setLoginType(String type) {
        preferences.edit().putString(KEY_LOGIN_TYPE, type).apply();
    }

    /**
     * Сохраняет сессию пользователя после успешного входа
     */
    public void saveUserSession(String email, String loginType) {
        setLoggedIn(true);
        setUserEmail(email);
        setLoginType(loginType);
    }

    /**
     * Очищает сессию пользователя (выход из аккаунта)
     */
    public void clearUserSession() {
        setLoggedIn(false);
        setUserEmail("");
        setLoginType("guest");
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Возвращает URL для подключения к ESP32
     */
    public String getEsp32Url() {
        if (isUseMdns()) {
            return "http://" + getMdnsHostname();
        } else {
            return "http://" + getEsp32Ip() + ":" + getEsp32Port();
        }
    }

    // ========== СБРОС НАСТРОЕК ==========

    public void resetToDefaults() {
        SharedPreferences.Editor editor = preferences.edit();
        // Сброс ESP32 настроек
        editor.putString(KEY_ESP32_IP, DEFAULT_ESP32_IP);
        editor.putInt(KEY_ESP32_PORT, DEFAULT_ESP32_PORT);
        editor.putBoolean(KEY_USE_MDNS, DEFAULT_USE_MDNS);
        editor.putString(KEY_MDNS_HOSTNAME, DEFAULT_MDNS_HOSTNAME);
        // Сброс настроек штор
        editor.putString(KEY_CURTAIN_ID, DEFAULT_CURTAIN_ID);
        editor.putString(KEY_CURTAIN_NAME, DEFAULT_CURTAIN_NAME);
        // Сброс настроек приложения
        editor.putInt(KEY_AUTO_UPDATE_INTERVAL, DEFAULT_AUTO_UPDATE_INTERVAL);
        editor.putInt(KEY_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        editor.putBoolean(KEY_ENABLE_VIBRATION, DEFAULT_ENABLE_VIBRATION);
        editor.putBoolean(KEY_ENABLE_SOUND, DEFAULT_ENABLE_SOUND);
        editor.putBoolean(KEY_SHOW_CONNECTION_ERRORS, DEFAULT_SHOW_CONNECTION_ERRORS);
        editor.putBoolean(KEY_FIRST_LAUNCH, false);
        // НЕ сбрасываем авторизацию при сбросе настроек
        editor.apply();
    }

    /**
     * Полный сброс ВСЕХ настроек (включая авторизацию)
     */
    public void resetAll() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    // ========== ЭКСПОРТ НАСТРОЕК ==========

    public String exportSettings() {
        return "=== НАСТРОЙКИ ESP32 ===\n" +
                "Режим mDNS: " + (isUseMdns() ? "Да" : "Нет") + "\n" +
                (isUseMdns() ? "mDNS имя: " + getMdnsHostname() : "IP адрес: " + getEsp32Ip()) + "\n" +
                "Порт: " + getEsp32Port() + "\n\n" +
                "=== НАСТРОЙКИ ШТОР ===\n" +
                "ID шторы: " + getDefaultCurtainId() + "\n" +
                "Название: " + getCurtainName() + "\n\n" +
                "=== НАСТРОЙКИ ПРИЛОЖЕНИЯ ===\n" +
                "Автообновление: " + getAutoUpdateInterval() + " мс\n" +
                "Таймаут: " + getConnectionTimeout() + " мс\n" +
                "Вибрация: " + (isVibrationEnabled() ? "Вкл" : "Выкл") + "\n" +
                "Звук: " + (isSoundEnabled() ? "Вкл" : "Выкл") + "\n" +
                "Показывать ошибки: " + (isShowConnectionErrors() ? "Да" : "Нет") + "\n\n" +
                "=== АВТОРИЗАЦИЯ ===\n" +
                "Вход выполнен: " + (isLoggedIn() ? "Да" : "Нет") + "\n" +
                "Email: " + getUserEmail() + "\n" +
                "Тип: " + getLoginType() + "\n\n" +
                "URL подключения: " + getEsp32Url();
    }
}