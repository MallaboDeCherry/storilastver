package com.example.sshtori.devices;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sshtori.R;
import com.example.sshtori.models.SmartDevice;
import com.google.android.material.snackbar.Snackbar;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "DeviceScan";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView statusText;
    private Button scanButton;

    private NsdManager nsdManager;
    private List<SmartDevice> discoveredDevices;
    private DeviceAdapter deviceAdapter;
    private Handler handler;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);

        initViews();
        setupToolbar();
        initComponents();
        setupListeners();

        checkPermissionsAndStart();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.devices_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        emptyText = findViewById(R.id.empty_text);
        statusText = findViewById(R.id.status_text);
        scanButton = findViewById(R.id.scan_button);

        if (statusText != null) {
            statusText.setText("Нажмите кнопку для поиска устройств");
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Поиск устройств");
        }
    }

    private void initComponents() {
        nsdManager = (NsdManager) getSystemService(NSD_SERVICE);
        discoveredDevices = new ArrayList<>();
        handler = new Handler();

        deviceAdapter = new DeviceAdapter(discoveredDevices, device -> {
            Log.d(TAG, "Выбрано устройство: " + (device != null ? device.getName() : "null"));

            if (device == null) {
                Toast.makeText(DeviceScanActivity.this, "Ошибка: устройство не найдено", Toast.LENGTH_SHORT).show();
                return;
            }

            if (device.getIpAddress() == null || device.getIpAddress().isEmpty()) {
                Toast.makeText(DeviceScanActivity.this, "Ошибка: IP адрес не найден", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("device", device);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(deviceAdapter);
    }

    private void setupListeners() {
        scanButton.setOnClickListener(v -> {
            if (isScanning) {
                stopScan();
            } else {
                startScan();
            }
        });
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Разрешения получены, но сканирование не запускаем автоматически
                if (statusText != null) {
                    statusText.setText("Нажмите кнопку для поиска устройств");
                }
            } else {
                Toast.makeText(this, "Разрешения необходимы для поиска устройств", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startScan() {
        if (isScanning) return;

        discoveredDevices.clear();
        deviceAdapter.updateDevices(discoveredDevices);
        showEmptyState(false);

        isScanning = true;
        progressBar.setVisibility(View.VISIBLE);
        scanButton.setText("Остановить");

        if (statusText != null) {
            statusText.setText("Поиск устройств...");
        }

        handler.postDelayed(() -> {
            if (isScanning) {
                stopScan();
                if (discoveredDevices.isEmpty()) {
                    showEmptyState(true);
                    if (statusText != null) {
                        statusText.setText("Устройства не найдены");
                    }
                }
            }
        }, 15000);

        try {
            nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка сканирования: " + e.getMessage());
            stopScan();
            Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopScan() {
        if (!isScanning) return;

        isScanning = false;
        progressBar.setVisibility(View.GONE);
        scanButton.setText("Найти устройства");

        if (statusText != null && discoveredDevices.isEmpty()) {
            statusText.setText("Нажмите кнопку для поиска устройств");
        } else if (statusText != null && !discoveredDevices.isEmpty()) {
            statusText.setText("Найдено устройств: " + discoveredDevices.size());
        }

        try {
            nsdManager.stopServiceDiscovery(discoveryListener);
        } catch (Exception ignored) {}

        if (discoveredDevices.isEmpty()) {
            showEmptyState(true);
            Snackbar.make(findViewById(android.R.id.content),
                    "Устройства не найдены. Убедитесь, что ESP32 включен и в одной сети",
                    Snackbar.LENGTH_LONG).show();
        } else {
            showEmptyState(false);
        }
    }

    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            runOnUiThread(() -> {
                stopScan();
                Toast.makeText(DeviceScanActivity.this, "Ошибка запуска сканирования", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {}

        @Override
        public void onDiscoveryStarted(String serviceType) {
            Log.d(TAG, "Discovery started");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.d(TAG, "Discovery stopped");
        }

        @Override
        public void onServiceFound(NsdServiceInfo serviceInfo) {
            String serviceName = serviceInfo.getServiceName();
            Log.d(TAG, "Service found: " + serviceName);

            if (serviceName.toLowerCase().contains("curtain") ||
                    serviceName.toLowerCase().contains("smart") ||
                    serviceName.toLowerCase().contains("esp")) {

                nsdManager.resolveService(serviceInfo, resolveListener);
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo serviceInfo) {
            Log.d(TAG, "Service lost: " + serviceInfo.getServiceName());
        }
    };

    private final NsdManager.ResolveListener resolveListener = new NsdManager.ResolveListener() {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed for: " + serviceInfo.getServiceName() + ", error: " + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            InetAddress address = serviceInfo.getHost();
            if (address != null) {
                String ipAddress = address.getHostAddress();
                String serviceName = serviceInfo.getServiceName();
                String mdnsName = serviceName + ".local";

                Log.d(TAG, "Service resolved: " + serviceName + " IP: " + ipAddress);

                if (!containsDeviceByIp(ipAddress)) {
                    SmartDevice device = new SmartDevice();
                    device.setId("device_" + System.currentTimeMillis());
                    device.setName("Умные шторы");
                    device.setIpAddress(ipAddress);
                    device.setPort(serviceInfo.getPort());
                    device.setMdnsHostname(mdnsName);
                    device.setType(SmartDevice.DeviceType.CURTAIN);
                    device.setConnected(true);

                    discoveredDevices.add(device);
                    runOnUiThread(() -> {
                        deviceAdapter.updateDevices(discoveredDevices);
                        showEmptyState(false);
                        if (statusText != null) {
                            statusText.setText("Найдено устройств: " + discoveredDevices.size());
                        }
                    });
                }
            }
        }
    };

    private boolean containsDeviceByIp(String ipAddress) {
        for (SmartDevice d : discoveredDevices) {
            if (ipAddress.equals(d.getIpAddress())) {
                return true;
            }
        }
        return false;
    }

    private void showEmptyState(boolean show) {
        if (emptyText == null || recyclerView == null) return;

        if (show && discoveredDevices.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isScanning) {
            stopScan();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}