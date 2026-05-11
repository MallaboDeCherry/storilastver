package com.example.sshtori.devices;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.sshtori.R;
import com.example.sshtori.models.CurtainState;
import com.example.sshtori.network.Esp32ApiClient;
import com.example.sshtori.utils.PreferencesManager;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceDetailActivity extends AppCompatActivity {

    private TextView deviceNameText;
    private TextView deviceStatusText;
    private TextView positionValueText;
    private TextView lastUpdateText;
    private TextView connectionText;
    private ImageView connectionIcon;
    private Slider positionSlider;
    private Button btnOpen, btnClose, btnStop;
    private Button btnQuick25, btnQuick50, btnQuick75;

    private Esp32ApiClient esp32Client;
    private PreferencesManager prefsManager;
    private CurtainState currentState;
    private Handler handler = new Handler();
    private boolean isDragging = false;
    private boolean isRefreshing = false;
    private boolean isDestroyed = false;  // ← Флаг для отслеживания состояния Activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        // Получаем данные из Intent
        String deviceIp = getIntent().getStringExtra("device_ip");
        String deviceName = getIntent().getStringExtra("device_name");
        int devicePort = getIntent().getIntExtra("device_port", 80);

        initViews();

        if (deviceName != null) {
            deviceNameText.setText(deviceName);
        }

        initClient(deviceIp, devicePort);
        setupListeners();

        checkConnection();
        startAutoRefresh();
    }

    private void initViews() {
        deviceNameText = findViewById(R.id.device_name_text);
        deviceStatusText = findViewById(R.id.device_status_text);
        positionValueText = findViewById(R.id.position_value_text);
        lastUpdateText = findViewById(R.id.last_update_text);
        connectionText = findViewById(R.id.connection_text);
        connectionIcon = findViewById(R.id.connection_icon);
        positionSlider = findViewById(R.id.position_slider);
        btnOpen = findViewById(R.id.btn_open);
        btnClose = findViewById(R.id.btn_close);
        btnStop = findViewById(R.id.btn_stop);
        btnQuick25 = findViewById(R.id.btn_quick_25);
        btnQuick50 = findViewById(R.id.btn_quick_50);
        btnQuick75 = findViewById(R.id.btn_quick_75);

        positionSlider.setEnabled(false);
    }

    private void initClient(String ip, int port) {
        prefsManager = new PreferencesManager(this);

        if (ip != null && !ip.isEmpty()) {
            esp32Client = new Esp32ApiClient(ip, port);
            connectionText.setText("Подключение к " + ip + ":" + port);
        } else {
            boolean useMdns = prefsManager.isUseMdns();
            if (useMdns) {
                esp32Client = new Esp32ApiClient("smart-curtain.local");
                connectionText.setText("Подключение к smart-curtain.local");
            } else {
                String savedIp = prefsManager.getEsp32Ip();
                int savedPort = prefsManager.getEsp32Port();
                esp32Client = new Esp32ApiClient(savedIp, savedPort);
                connectionText.setText("Подключение к " + savedIp + ":" + savedPort);
            }
        }
    }

    private void setupListeners() {
        btnOpen.setOnClickListener(v -> {
            if (isDestroyed || esp32Client == null) return;
            btnOpen.setEnabled(false);
            esp32Client.open(new Esp32ApiClient.SimpleCallback() {
                @Override
                public void onSuccess() {
                    if (!isDestroyed) {
                        runOnUiThread(() -> {
                            btnOpen.setEnabled(true);
                            refreshState();
                            Toast.makeText(DeviceDetailActivity.this, "Открытие...", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                @Override
                public void onError(String error) {
                    if (!isDestroyed) {
                        runOnUiThread(() -> {
                            btnOpen.setEnabled(true);
                            showError(error);
                        });
                    }
                }
            });
        });

        btnClose.setOnClickListener(v -> {
            if (isDestroyed || esp32Client == null) return;
            btnClose.setEnabled(false);
            esp32Client.close(new Esp32ApiClient.SimpleCallback() {
                @Override
                public void onSuccess() {
                    if (!isDestroyed) {
                        runOnUiThread(() -> {
                            btnClose.setEnabled(true);
                            refreshState();
                            Toast.makeText(DeviceDetailActivity.this, "Закрытие...", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                @Override
                public void onError(String error) {
                    if (!isDestroyed) {
                        runOnUiThread(() -> {
                            btnClose.setEnabled(true);
                            showError(error);
                        });
                    }
                }
            });
        });

        btnStop.setOnClickListener(v -> {
            if (isDestroyed || esp32Client == null) return;
            esp32Client.stop(new Esp32ApiClient.SimpleCallback() {
                @Override
                public void onSuccess() {
                    if (!isDestroyed) {
                        runOnUiThread(() -> refreshState());
                    }
                }
                @Override
                public void onError(String error) {
                    if (!isDestroyed) {
                        runOnUiThread(() -> showError(error));
                    }
                }
            });
        });

        btnQuick25.setOnClickListener(v -> setPosition(25));
        btnQuick50.setOnClickListener(v -> setPosition(50));
        btnQuick75.setOnClickListener(v -> setPosition(75));

        positionSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                positionValueText.setText((int) value + "%");
            }
        });

        positionSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                isDragging = true;
            }
            @Override
            public void onStopTrackingTouch(Slider slider) {
                isDragging = false;
                setPosition((int) slider.getValue());
            }
        });
    }

    private void setPosition(int pos) {
        if (isDestroyed || esp32Client == null) return;
        positionSlider.setEnabled(false);
        esp32Client.setPosition(pos, new Esp32ApiClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isDestroyed) {
                    runOnUiThread(() -> {
                        positionSlider.setEnabled(true);
                        refreshState();
                    });
                }
            }
            @Override
            public void onError(String error) {
                if (!isDestroyed) {
                    runOnUiThread(() -> {
                        positionSlider.setEnabled(true);
                        showError(error);
                    });
                }
            }
        });
    }

    private void checkConnection() {
        if (isDestroyed || esp32Client == null) return;
        connectionText.setText("Проверка...");

        esp32Client.ping(new Esp32ApiClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (!isDestroyed) {
                    runOnUiThread(() -> {
                        connectionText.setText("ESP32 онлайн");
                        positionSlider.setEnabled(true);
                        refreshState();
                    });
                }
            }
            @Override
            public void onError(String error) {
                if (!isDestroyed) {
                    runOnUiThread(() -> {
                        connectionText.setText("Ошибка: " + error);
                        positionSlider.setEnabled(false);
                    });
                }
            }
        });
    }

    private void refreshState() {
        if (isRefreshing || isDestroyed || esp32Client == null) return;
        isRefreshing = true;

        esp32Client.getStatus(new Esp32ApiClient.StatusCallback() {
            @Override
            public void onSuccess(CurtainState state) {
                if (!isDestroyed) {
                    runOnUiThread(() -> {
                        updateUI(state);
                        isRefreshing = false;
                    });
                }
            }
            @Override
            public void onError(String error) {
                if (!isDestroyed) {
                    runOnUiThread(() -> {
                        connectionText.setText("Ошибка: " + error);
                        isRefreshing = false;
                    });
                }
            }
        });
    }

    private void updateUI(CurtainState state) {
        currentState = state;
        deviceStatusText.setText(state.getFormattedStatus());

        int position = state.getPosition();
        positionValueText.setText(String.valueOf(position));

        if (!isDragging) {
            positionSlider.setValue(position);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateText.setText(sdf.format(new Date()));

        if (state.isMoving()) {
            deviceStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
        } else if (state.isOpened()) {
            deviceStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else if (state.isClosed()) {
            deviceStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
        } else {
            deviceStatusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
        }

        if (state.hasError()) {
            connectionText.setText("Ошибка устройства");
        } else if (state.isMoving()) {
            connectionText.setText("Движется...");
        } else {
            connectionText.setText("ESP32 онлайн");
        }
    }

    private void startAutoRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isDestroyed) {
                    refreshState();
                    handler.postDelayed(this, 2000);
                }
            }
        }, 2000);
    }

    private void showError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(DeviceDetailActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            connectionText.setText("Ошибка: " + error);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDestroyed = false;
        startAutoRefresh();
        checkConnection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        if (esp32Client != null) {
            esp32Client.shutdown();
        }
        handler.removeCallbacksAndMessages(null);
    }
}