package com.example.smartcurtain.network;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class MdnsDiscovery {
    private static final String TAG = "MdnsDiscovery";
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String ESP32_SERVICE_NAME = "smart-curtain";

    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.ResolveListener resolveListener;
    private DiscoveryCallback callback;
    private boolean isDiscovering = false;

    public interface DiscoveryCallback {
        void onDeviceFound(String name, String ipAddress, int port);
        void onDiscoveryError(String error);
        void onDiscoveryFinished();
    }

    public MdnsDiscovery(Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void startDiscovery(DiscoveryCallback callback) {
        this.callback = callback;

        if (isDiscovering) {
            stopDiscovery();
        }

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery start failed: " + errorCode);
                isDiscovering = false;
                if (callback != null) {
                    callback.onDiscoveryError("Failed to start discovery: " + errorCode);
                }
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery stop failed: " + errorCode);
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Discovery started for: " + serviceType);
                isDiscovering = true;
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Discovery stopped");
                isDiscovering = false;
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service found: " + serviceInfo.getServiceName());

                // Проверяем, что это наша штора
                if (serviceInfo.getServiceName().toLowerCase().contains("curtain") ||
                        serviceInfo.getServiceType().equals(SERVICE_TYPE)) {

                    // Разрешаем сервис для получения IP
                    resolveListener = new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve failed: " + errorCode);
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            String ipAddress = serviceInfo.getHost().getHostAddress();
                            int port = serviceInfo.getPort();

                            Log.d(TAG, String.format("Resolved: %s at %s:%d",
                                    serviceInfo.getServiceName(), ipAddress, port));

                            if (callback != null) {
                                callback.onDeviceFound(serviceInfo.getServiceName(), ipAddress, port);
                            }
                        }
                    };

                    nsdManager.resolveService(serviceInfo, resolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service lost: " + serviceInfo.getServiceName());
            }
        };

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void stopDiscovery() {
        if (discoveryListener != null && isDiscovering) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Stop discovery failed", e);
            }
            isDiscovering = false;
        }
    }

    // Ручной поиск через mDNS hostname (альтернативный метод)
    public static String resolveMdnsHostname(String hostname) {
        // В Android mDNS разрешается автоматически через DNS
        // Просто возвращаем hostname как есть, система сама его разрешит
        return hostname;
    }
}