package com.example.sshtori.devices;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sshtori.R;
import com.example.sshtori.WiFiSetupActivity;
import com.example.sshtori.models.SmartDevice;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DeviceHubActivity extends AppCompatActivity {

    private static final String TAG = "DeviceHub";
    private static final int REQUEST_SCAN_DEVICE = 100;

    private RecyclerView devicesGrid;
    private ExtendedFloatingActionButton fabAddDevice;
    private TextView emptyText;

    private DeviceGridAdapter deviceAdapter;
    private List<SmartDevice> devicesList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_hub);

        prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        initializeViews();
        setupRecyclerView();
        loadDevices();
        setupListeners();

        checkEmptyState();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Мои устройства");
        }

        devicesGrid = findViewById(R.id.devices_grid);
        fabAddDevice = findViewById(R.id.fab_add_device);
        emptyText = findViewById(R.id.empty_text);
    }

    private void setupRecyclerView() {
        devicesGrid.setLayoutManager(new GridLayoutManager(this, 2));
        deviceAdapter = new DeviceGridAdapter(
                device -> {
                    Log.d(TAG, "Нажатие на устройство: " + device.getName());
                    openDeviceControl(device);
                },
                device -> {
                    Log.d(TAG, "Долгое нажатие на устройство: " + device.getName());
                    showDeviceOptions(device);
                }
        );
        devicesGrid.setAdapter(deviceAdapter);
    }

    private void loadDevices() {
        devicesList = new ArrayList<>();

        String devicesJson = prefs.getString("saved_devices", "");
        if (!devicesJson.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(devicesJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonDevice = jsonArray.getJSONObject(i);
                    SmartDevice device = new SmartDevice();
                    device.setId(jsonDevice.getString("id"));
                    device.setName(jsonDevice.getString("name"));
                    device.setIpAddress(jsonDevice.getString("ipAddress"));
                    device.setPort(jsonDevice.getInt("port"));
                    device.setMdnsHostname(jsonDevice.optString("mdnsHostname", null));
                    device.setType(SmartDevice.DeviceType.valueOf(jsonDevice.getString("type")));
                    device.setConnected(true);
                    devicesList.add(device);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        deviceAdapter.updateDevices(devicesList);
    }

    private void saveDevices() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (SmartDevice device : devicesList) {
                JSONObject jsonDevice = new JSONObject();
                jsonDevice.put("id", device.getId());
                jsonDevice.put("name", device.getName());
                jsonDevice.put("ipAddress", device.getIpAddress());
                jsonDevice.put("port", device.getPort());
                if (device.getMdnsHostname() != null) {
                    jsonDevice.put("mdnsHostname", device.getMdnsHostname());
                }
                jsonDevice.put("type", device.getType().name());
                jsonArray.put(jsonDevice);
            }
            prefs.edit().putString("saved_devices", jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupListeners() {
        fabAddDevice.setOnClickListener(v -> openAddDeviceScreen());
    }

    private void openAddDeviceScreen() {
        String[] options = {"Поиск устройств в сети", "Настройка нового ESP32 через BLE"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить устройство");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(DeviceHubActivity.this, DeviceScanActivity.class);
                startActivityForResult(intent, REQUEST_SCAN_DEVICE);
            } else {
                startActivity(new Intent(DeviceHubActivity.this, WiFiSetupActivity.class));
            }
        });
        builder.show();
    }

    private void openDeviceControl(SmartDevice device) {
        if (device == null) {
            Toast.makeText(this, "Ошибка: устройство не найдено", Toast.LENGTH_SHORT).show();
            return;
        }

        if (device.getIpAddress() == null || device.getIpAddress().isEmpty()) {
            Toast.makeText(this, "Ошибка: IP адрес не указан", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(DeviceHubActivity.this, DeviceDetailActivity.class);
        intent.putExtra("device_ip", device.getIpAddress());
        intent.putExtra("device_port", device.getPort());
        intent.putExtra("device_name", device.getName());
        startActivity(intent);
    }

    private void showDeviceOptions(SmartDevice device) {
        new AlertDialog.Builder(this)
                .setTitle(device.getName())
                .setItems(new String[]{"Редактировать", "Удалить"}, (dialog, which) -> {
                    if (which == 0) {
                        editDevice(device);
                    } else {
                        deleteDevice(device);
                    }
                })
                .show();
    }

    private void editDevice(SmartDevice device) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_device, null);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.et_device_name);
        EditText etIp = dialogView.findViewById(R.id.et_device_ip);
        Spinner spinnerType = dialogView.findViewById(R.id.spinner_device_type);

        etName.setText(device.getName());
        etIp.setText(device.getIpAddress());

        ArrayAdapter<SmartDevice.DeviceType> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SmartDevice.DeviceType.values());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        spinnerType.setSelection(device.getType().ordinal());

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String newName = etName.getText().toString().trim();
            String newIp = etIp.getText().toString().trim();
            SmartDevice.DeviceType newType = (SmartDevice.DeviceType) spinnerType.getSelectedItem();

            if (newName.isEmpty()) {
                etName.setError("Введите название");
                return;
            }
            if (newIp.isEmpty()) {
                etIp.setError("Введите IP");
                return;
            }

            device.setName(newName);
            device.setIpAddress(newIp);
            device.setType(newType);

            deviceAdapter.updateDevices(devicesList);
            saveDevices();

            Toast.makeText(this, "Устройство обновлено", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void deleteDevice(SmartDevice device) {
        devicesList.remove(device);
        deviceAdapter.updateDevices(devicesList);
        saveDevices();
        checkEmptyState();
        Toast.makeText(this, "Устройство удалено", Toast.LENGTH_SHORT).show();
    }

    private void checkEmptyState() {
        if (devicesList.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            devicesGrid.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            devicesGrid.setVisibility(View.VISIBLE);
        }
    }

    // Проверка, есть ли уже устройство в списке
    private boolean containsDevice(SmartDevice device) {
        for (SmartDevice d : devicesList) {
            if (d.getIpAddress() != null && d.getIpAddress().equals(device.getIpAddress())) {
                return true;
            }
            if (d.getMdnsHostname() != null && d.getMdnsHostname().equals(device.getMdnsHostname())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_hub_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_SCAN_DEVICE && resultCode == RESULT_OK && data != null) {
            SmartDevice device = data.getParcelableExtra("device");
            if (device != null) {
                Log.d(TAG, "Получено устройство: " + device.getName() + ", IP: " + device.getIpAddress());

                if (!containsDevice(device)) {
                    devicesList.add(device);
                    saveDevices();
                    deviceAdapter.updateDevices(devicesList);
                    checkEmptyState();
                    Snackbar.make(findViewById(android.R.id.content),
                            "Устройство добавлено: " + device.getName(), Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(findViewById(android.R.id.content),
                            "Устройство уже существует: " + device.getName(), Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Device is null in onActivityResult");
                Toast.makeText(this, "Ошибка добавления устройства", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadDevices();
            deviceAdapter.updateDevices(devicesList);
            checkEmptyState();
            Snackbar.make(findViewById(android.R.id.content), "Список обновлен", Snackbar.LENGTH_SHORT).show();
            return true;
        }
        if (item.getItemId() == R.id.action_wifi_setup) {
            startActivity(new Intent(DeviceHubActivity.this, WiFiSetupActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (deviceAdapter != null) {
            loadDevices();
            deviceAdapter.updateDevices(devicesList);
            checkEmptyState();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}