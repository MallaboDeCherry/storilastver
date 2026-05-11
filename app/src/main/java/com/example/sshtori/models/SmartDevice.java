package com.example.sshtori.models;

import android.os.Parcel;
import android.os.Parcelable;

public class SmartDevice implements Parcelable {

    public enum DeviceType {
        CURTAIN("Штора"),
        LIGHT("Свет"),
        SOCKET("Розетка"),
        SENSOR("Датчик");

        private final String displayName;

        DeviceType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String id;
    private String name;
    private String ipAddress;
    private int port;
    private String mdnsHostname;
    private DeviceType type;
    private boolean isConnected;

    public SmartDevice() {
        this.port = 80;
        this.type = DeviceType.CURTAIN;
        this.isConnected = false;
    }

    public SmartDevice(String id, String name, String ipAddress, DeviceType type) {
        this.id = id;
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = 80;
        this.type = type;
        this.isConnected = false;
    }

    protected SmartDevice(Parcel in) {
        id = in.readString();
        name = in.readString();
        ipAddress = in.readString();
        port = in.readInt();
        mdnsHostname = in.readString();
        type = DeviceType.valueOf(in.readString());
        isConnected = in.readByte() != 0;
    }

    public static final Creator<SmartDevice> CREATOR = new Creator<SmartDevice>() {
        @Override
        public SmartDevice createFromParcel(Parcel in) {
            return new SmartDevice(in);
        }

        @Override
        public SmartDevice[] newArray(int size) {
            return new SmartDevice[size];
        }
    };

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }
    public String getMdnsHostname() { return mdnsHostname; }
    public DeviceType getType() { return type; }
    public boolean isConnected() { return isConnected; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setPort(int port) { this.port = port; }
    public void setMdnsHostname(String mdnsHostname) { this.mdnsHostname = mdnsHostname; }
    public void setType(DeviceType type) { this.type = type; }
    public void setConnected(boolean connected) { isConnected = connected; }

    public String getBaseUrl() {
        if (mdnsHostname != null && !mdnsHostname.isEmpty()) {
            return "http://" + mdnsHostname;
        }
        return "http://" + ipAddress + ":" + port;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(ipAddress);
        dest.writeInt(port);
        dest.writeString(mdnsHostname);
        dest.writeString(type.name());
        dest.writeByte((byte) (isConnected ? 1 : 0));
    }

    @Override
    public String toString() {
        return name + " (" + type.getDisplayName() + ") - " + getBaseUrl();
    }
}