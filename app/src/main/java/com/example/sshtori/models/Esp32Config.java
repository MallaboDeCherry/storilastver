package com.example.smartcurtain.models;

import java.io.Serializable;

public class Esp32Config implements Serializable {
    private String deviceName;
    private String ipAddress;
    private int port;
    private String mdnsHostname;  // smart-curtain.local
    private boolean useMdns;
    private int connectionTimeout;
    private int pollingInterval;
    private boolean enableVibration;
    private boolean enableNotifications;
    private boolean autoReconnect;
    private int maxRetries;

    // Значения по умолчанию
    public static final int DEFAULT_PORT = 80;
    public static final int DEFAULT_TIMEOUT = 3000;
    public static final int DEFAULT_POLLING_INTERVAL = 1000;
    public static final String DEFAULT_MDNS_NAME = "smart-curtain.local";

    public Esp32Config() {
        this.deviceName = "Умные шторы";
        this.ipAddress = "192.168.1.100";
        this.port = DEFAULT_PORT;
        this.mdnsHostname = DEFAULT_MDNS_NAME;
        this.useMdns = true;
        this.connectionTimeout = DEFAULT_TIMEOUT;
        this.pollingInterval = DEFAULT_POLLING_INTERVAL;
        this.enableVibration = true;
        this.enableNotifications = true;
        this.autoReconnect = true;
        this.maxRetries = 3;
    }

    // Геттеры и сеттеры
    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getMdnsHostname() { return mdnsHostname; }
    public void setMdnsHostname(String mdnsHostname) { this.mdnsHostname = mdnsHostname; }

    public boolean isUseMdns() { return useMdns; }
    public void setUseMdns(boolean useMdns) { this.useMdns = useMdns; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getPollingInterval() { return pollingInterval; }
    public void setPollingInterval(int pollingInterval) { this.pollingInterval = pollingInterval; }

    public boolean isEnableVibration() { return enableVibration; }
    public void setEnableVibration(boolean enableVibration) { this.enableVibration = enableVibration; }

    public boolean isEnableNotifications() { return enableNotifications; }
    public void setEnableNotifications(boolean enableNotifications) { this.enableNotifications = enableNotifications; }

    public boolean isAutoReconnect() { return autoReconnect; }
    public void setAutoReconnect(boolean autoReconnect) { this.autoReconnect = autoReconnect; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public String getBaseUrl() {
        return "http://" + ipAddress + ":" + port;
    }

    @Override
    public String toString() {
        return String.format("Esp32Config{name='%s', url='%s', useMdns=%b}",
                deviceName, getBaseUrl(), useMdns);
    }
}