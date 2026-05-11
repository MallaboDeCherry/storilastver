package com.example.sshtori;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sshtori.utils.PreferencesManager;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    // Настройки ESP32
    private TextInputEditText esp32IpInput;
    private TextInputEditText esp32PortInput;
    private SwitchMaterial useMdnsSwitch;
    private TextInputEditText mdnsHostnameInput;

    // Настройки штор
    private TextInputEditText curtainNameInput;
    private TextInputEditText curtainIdInput;

    // Настройки приложения
    private TextInputEditText autoUpdateIntervalInput;
    private TextInputEditText connectionTimeoutInput;
    private SwitchMaterial vibrationSwitch;
    private SwitchMaterial soundSwitch;

    // Кнопки
    private Button saveButton;
    private Button resetButton;

    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Настройка тулбара
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("Настройки");
            }
        }

        preferencesManager = new PreferencesManager(this);

        initializeViews();
        loadSettings();
        setupListeners();
    }

    private void initializeViews() {
        // ESP32 настройки
        esp32IpInput = findViewById(R.id.esp32_ip_input);
        esp32PortInput = findViewById(R.id.esp32_port_input);
        useMdnsSwitch = findViewById(R.id.use_mdns_switch);
        mdnsHostnameInput = findViewById(R.id.mdns_hostname_input);

        // Настройки штор
        curtainNameInput = findViewById(R.id.curtain_name_input);
        curtainIdInput = findViewById(R.id.curtain_id_input);

        // Настройки приложения
        autoUpdateIntervalInput = findViewById(R.id.auto_update_interval_input);
        connectionTimeoutInput = findViewById(R.id.connection_timeout_input);
        vibrationSwitch = findViewById(R.id.vibration_switch);
        soundSwitch = findViewById(R.id.sound_switch);

        // Кнопки
        saveButton = findViewById(R.id.save_button);
        resetButton = findViewById(R.id.reset_button);
    }

    private void loadSettings() {
        try {
            // ESP32 настройки
            if (esp32IpInput != null) esp32IpInput.setText(preferencesManager.getEsp32Ip());
            if (esp32PortInput != null) esp32PortInput.setText(String.valueOf(preferencesManager.getEsp32Port()));
            if (useMdnsSwitch != null) useMdnsSwitch.setChecked(preferencesManager.isUseMdns());
            if (mdnsHostnameInput != null) mdnsHostnameInput.setText(preferencesManager.getMdnsHostname());

            // Настройки штор
            if (curtainNameInput != null) curtainNameInput.setText(preferencesManager.getCurtainName());
            if (curtainIdInput != null) curtainIdInput.setText(preferencesManager.getDefaultCurtainId());

            // Настройки приложения
            if (autoUpdateIntervalInput != null) autoUpdateIntervalInput.setText(String.valueOf(preferencesManager.getAutoUpdateInterval()));
            if (connectionTimeoutInput != null) connectionTimeoutInput.setText(String.valueOf(preferencesManager.getConnectionTimeout()));
            if (vibrationSwitch != null) vibrationSwitch.setChecked(preferencesManager.isVibrationEnabled());
            if (soundSwitch != null) soundSwitch.setChecked(preferencesManager.isSoundEnabled());

            // Обновляем UI в зависимости от режима mDNS
            updateMdnsUi();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка загрузки настроек: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        // Переключение режима mDNS
        if (useMdnsSwitch != null) {
            useMdnsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateMdnsUi();
            });
        }

        // Кнопка сохранения
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveSettings());
        }

        // Кнопка сброса
        if (resetButton != null) {
            resetButton.setOnClickListener(v -> resetSettings());
        }
    }

    private void updateMdnsUi() {
        if (useMdnsSwitch == null) return;

        boolean useMdns = useMdnsSwitch.isChecked();
        if (mdnsHostnameInput != null) mdnsHostnameInput.setEnabled(useMdns);
        if (esp32IpInput != null) esp32IpInput.setEnabled(!useMdns);
        if (esp32PortInput != null) esp32PortInput.setEnabled(!useMdns);

        if (useMdns && mdnsHostnameInput != null) {
            mdnsHostnameInput.setHint("smart-curtain.local");
        } else if (esp32IpInput != null) {
            esp32IpInput.setHint("192.168.1.100");
        }
    }

    private void saveSettings() {
        try {
            // Сохраняем ESP32 настройки
            if (esp32IpInput != null) {
                preferencesManager.setEsp32Ip(esp32IpInput.getText().toString());
            }

            if (esp32PortInput != null) {
                try {
                    int port = Integer.parseInt(esp32PortInput.getText().toString());
                    preferencesManager.setEsp32Port(port);
                } catch (NumberFormatException e) {
                    preferencesManager.setEsp32Port(80);
                }
            }

            if (useMdnsSwitch != null) {
                preferencesManager.setUseMdns(useMdnsSwitch.isChecked());
            }

            if (mdnsHostnameInput != null) {
                preferencesManager.setMdnsHostname(mdnsHostnameInput.getText().toString());
            }

            // Сохраняем настройки штор
            if (curtainNameInput != null) {
                preferencesManager.setCurtainName(curtainNameInput.getText().toString());
            }

            if (curtainIdInput != null) {
                preferencesManager.setDefaultCurtainId(curtainIdInput.getText().toString());
            }

            // Сохраняем настройки приложения
            if (autoUpdateIntervalInput != null) {
                try {
                    int interval = Integer.parseInt(autoUpdateIntervalInput.getText().toString());
                    preferencesManager.setAutoUpdateInterval(interval);
                } catch (NumberFormatException e) {
                    preferencesManager.setAutoUpdateInterval(2000);
                }
            }

            if (connectionTimeoutInput != null) {
                try {
                    int timeout = Integer.parseInt(connectionTimeoutInput.getText().toString());
                    preferencesManager.setConnectionTimeout(timeout);
                } catch (NumberFormatException e) {
                    preferencesManager.setConnectionTimeout(3000);
                }
            }

            if (vibrationSwitch != null) {
                preferencesManager.setVibrationEnabled(vibrationSwitch.isChecked());
            }

            if (soundSwitch != null) {
                preferencesManager.setSoundEnabled(soundSwitch.isChecked());
            }

            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void resetSettings() {
        try {
            preferencesManager.resetToDefaults();
            loadSettings();
            Toast.makeText(this, "Настройки сброшены к стандартным", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка сброса: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}