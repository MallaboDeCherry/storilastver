package com.example.sshtori.devices;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.sshtori.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WiFiSetupActivity extends AppCompatActivity {

    // BLE UUIDs (должны совпадать с ESP32)
    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    // UI элементы
    private Toolbar toolbar;
    private TextView statusText;
    private ProgressBar progressBar;
    private LinearLayout scanLayout;
    private LinearLayout configLayout;
    private ListView devicesListView;
    private EditText ssidInput;
    private EditText passwordInput;
    private Button scanButton;
    private Button connectButton;
    private TextView selectedDeviceText;

    // BLE переменные
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Map<String, String> devicesMap = new HashMap<>();
    private ArrayAdapter<String> devicesAdapter;
    private List<String> deviceNames = new ArrayList<>();
    private boolean isScanning = false;
    private String selectedDeviceAddress = null;
    private String selectedDeviceName = null;

    // Wi-Fi переменные
    private WifiManager wifiManager;
    private List<String> wifiNetworks = new ArrayList<>();
    private ArrayAdapter<String> wifiAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_setup);

        initViews();
        setupToolbar();
        checkPermissions();
        setupListeners();

        scanLayout.setVisibility(View.VISIBLE);
        configLayout.setVisibility(View.GONE);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        statusText = findViewById(R.id.status_text);
        progressBar = findViewById(R.id.progress_bar);
        scanLayout = findViewById(R.id.scan_layout);
        configLayout = findViewById(R.id.config_layout);
        devicesListView = findViewById(R.id.devices_list);
        ssidInput = findViewById(R.id.ssid_input);
        passwordInput = findViewById(R.id.password_input);
        scanButton = findViewById(R.id.scan_button);
        connectButton = findViewById(R.id.connect_button);
        selectedDeviceText = findViewById(R.id.selected_device_text);

        devicesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        devicesListView.setAdapter(devicesAdapter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Настройка Wi-Fi для ESP32");
        }
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 100);
        } else {
            checkBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            checkBluetooth();
        }
    }

    private void checkBluetooth() {
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = manager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        } else {
            startScan();
        }
    }

    private void setupListeners() {
        scanButton.setOnClickListener(v -> startScan());

        devicesListView.setOnItemClickListener((parent, view, position, id) -> {
            selectedDeviceName = deviceNames.get(position);
            selectedDeviceAddress = getKeyByValue(selectedDeviceName);
            if (selectedDeviceAddress != null) {
                selectedDeviceText.setText("Выбрано: " + selectedDeviceName);
                connectToESP32(selectedDeviceAddress);
            }
        });

        connectButton.setOnClickListener(v -> {
            String ssid = ssidInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (ssid.isEmpty()) {
                Toast.makeText(this, "Введите название Wi-Fi сети", Toast.LENGTH_SHORT).show();
                return;
            }

            sendWiFiCredentials(ssid, password);
        });
    }

    private void startScan() {
        if (isScanning) {
            stopScan();
            return;
        }

        deviceNames.clear();
        devicesMap.clear();
        devicesAdapter.notifyDataSetChanged();

        statusText.setText("🔍 Поиск ESP32...\n(Убедитесь, что ESP32 включен)");
        progressBar.setVisibility(View.VISIBLE);
        scanButton.setEnabled(false);
        isScanning = true;

        try {
            bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
            handler.postDelayed(this::stopScan, 15000);
        } catch (SecurityException e) {
            statusText.setText("❌ Нет разрешения на сканирование");
            progressBar.setVisibility(View.GONE);
            scanButton.setEnabled(true);
            isScanning = false;
        }
    }

    private void stopScan() {
        if (isScanning) {
            try {
                bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            } catch (Exception ignored) {}

            isScanning = false;
            progressBar.setVisibility(View.GONE);
            scanButton.setEnabled(true);

            if (deviceNames.isEmpty()) {
                statusText.setText("❌ Устройства не найдены\n\nУбедитесь, что:\n• ESP32 включен\n• На ESP32 горит светодиод\n• ESP32 в режиме настройки");
            } else {
                statusText.setText("✅ Найдено " + deviceNames.size() + " устройств\nНажмите на устройство для подключения");
                devicesListView.setVisibility(View.VISIBLE);
            }
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();
            String deviceAddress = device.getAddress();

            if (deviceName != null && (deviceName.contains("SmartCurtain") ||
                    deviceName.contains("ESP32") || deviceName.contains("smart"))) {
                if (!devicesMap.containsKey(deviceAddress)) {
                    devicesMap.put(deviceAddress, deviceName);
                    deviceNames.add(deviceName + "\n" + deviceAddress);
                    runOnUiThread(() -> devicesAdapter.notifyDataSetChanged());
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() -> {
                stopScan();
                statusText.setText("❌ Ошибка сканирования BLE");
                Toast.makeText(WiFiSetupActivity.this, "Ошибка сканирования", Toast.LENGTH_LONG).show();
            });
        }
    };

    private void connectToESP32(String deviceAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        statusText.setText("🔌 Подключение к ESP32...");
        progressBar.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Нет разрешения Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        bluetoothGatt = device.connectGatt(this, false, gattCallback);

        handler.postDelayed(() -> {
            runOnUiThread(() -> {
                scanLayout.setVisibility(View.GONE);
                configLayout.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                statusText.setText("✅ Подключено к " + selectedDeviceName + "\nВведите данные вашей Wi-Fi сети");
            });
        }, 2000);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread(() -> statusText.setText("✅ Подключено! Запрос сервисов..."));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(WiFiSetupActivity.this,
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> statusText.setText("✅ Устройство готово к настройке"));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("✅ Wi-Fi данные отправлены!\nESP32 перезагрузится и подключится к вашей сети");
                    Toast.makeText(WiFiSetupActivity.this,
                            "Настройки отправлены! ESP32 подключится к Wi-Fi через ~30 секунд",
                            Toast.LENGTH_LONG).show();
                    handler.postDelayed(() -> finish(), 4000);
                });
            } else {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("❌ Ошибка отправки данных");
                    Toast.makeText(WiFiSetupActivity.this, "Ошибка отправки", Toast.LENGTH_LONG).show();
                    connectButton.setEnabled(true);
                });
            }
            gatt.disconnect();
            gatt.close();
        }
    };

    private void sendWiFiCredentials(String ssid, String password) {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "Нет подключения к ESP32", Toast.LENGTH_LONG).show();
            return;
        }

        BluetoothGattService service = bluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            Toast.makeText(this, "Сервис не найден. Переподключитесь.", Toast.LENGTH_LONG).show();
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Toast.makeText(this, "Характеристика не найдена", Toast.LENGTH_LONG).show();
            return;
        }

        // Формат: "SSID:PASSWORD"
        String data = ssid + ":" + password;
        characteristic.setValue(data);

        statusText.setText("📡 Отправка данных Wi-Fi...");
        progressBar.setVisibility(View.VISIBLE);
        connectButton.setEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            progressBar.setVisibility(View.GONE);
            statusText.setText("❌ Нет разрешения на отправку");
            connectButton.setEnabled(true);
            return;
        }

        bluetoothGatt.writeCharacteristic(characteristic);
    }

    private String getKeyByValue(String value) {
        for (Map.Entry<String, String> entry : devicesMap.entrySet()) {
            String deviceInfo = entry.getValue() + "\n" + entry.getKey();
            if (deviceInfo.equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScan();
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            } catch (Exception ignored) {}
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}