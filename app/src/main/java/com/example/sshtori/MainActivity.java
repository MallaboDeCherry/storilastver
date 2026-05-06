package com.example.sshtori;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sshtori.models.CurtainCommand;
import com.example.sshtori.models.CurtainState;
import com.example.sshtori.models.ServerResponse;
import com.example.sshtori.network.CurtainApiClient;
import com.example.sshtori.utils.PreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Главная активность приложения для управления умными шторами
 */
public class MainActivity extends AppCompatActivity {

    // UI элементы
    private ImageView connectionStatusIcon;
    private TextView connectionStatusText;
    private TextView curtainNameText;
    private TextView curtainStatusText;
    private TextView positionValueText;
    private TextView serverUrlText;
    private TextView lastUpdateText;
    private Slider positionSlider;
    private MaterialButton btnOpen, btnClose, btnStop, btnRefresh;
    private MaterialButton btnQuick25, btnQuick50, btnQuick75;
    private FloatingActionButton fab;

    // Бизнес-логика
    private CurtainApiClient apiClient;
    private PreferencesManager preferencesManager;
    private CurtainState currentState;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private boolean isSliderBeingDragged = false;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация
        initializeViews();
        initializeManagers();
        setupListeners();

        // Первоначальная проверка соединения
        checkConnection();

        // Автоматическое обновление каждые 5 секунд
        startAutoUpdate();
    }

    /**
     * Инициализация UI элементов
     */
    private void initializeViews() {
        connectionStatusIcon = findViewById(R.id.connectionStatusIcon);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        curtainNameText = findViewById(R.id.curtainNameText);
        curtainStatusText = findViewById(R.id.curtainStatusText);
        positionValueText = findViewById(R.id.positionValueText);
        serverUrlText = findViewById(R.id.serverUrlText);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        positionSlider = findViewById(R.id.positionSlider);

        btnOpen = findViewById(R.id.btnOpen);
        btnClose = findViewById(R.id.btnClose);
        btnStop = findViewById(R.id.btnStop);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnQuick25 = findViewById(R.id.btnQuick25);
        btnQuick50 = findViewById(R.id.btnQuick50);
        btnQuick75 = findViewById(R.id.btnQuick75);
        fab = findViewById(R.id.fab);
    }

    /**
     * Инициализация менеджеров
     */
    private void initializeManagers() {
        preferencesManager = new PreferencesManager(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        String serverUrl = preferencesManager.getServerUrl();
        apiClient = new CurtainApiClient(serverUrl);

        serverUrlText.setText("Сервер: " + serverUrl);

        currentState = new CurtainState();
        currentState.setId(preferencesManager.getDefaultCurtainId());
        currentState.setName(preferencesManager.getCurtainName());
    }

    /**
     * Настройка слушателей событий
     */
    private void setupListeners() {
        // Кнопки управления
        btnOpen.setOnClickListener(v -> {
            vibrateIfEnabled();
            sendCommand("open");
        });
        btnClose.setOnClickListener(v -> {
            vibrateIfEnabled();
            sendCommand("close");
        });
        btnStop.setOnClickListener(v -> {
            vibrateIfEnabled();
            sendCommand("stop");
        });
        btnRefresh.setOnClickListener(v -> {
            vibrateIfEnabled();
            refreshCurtainState();
        });

        // Быстрые действия
        btnQuick25.setOnClickListener(v -> {
            vibrateIfEnabled();
            setPosition(25);
        });
        btnQuick50.setOnClickListener(v -> {
            vibrateIfEnabled();
            setPosition(50);
        });
        btnQuick75.setOnClickListener(v -> {
            vibrateIfEnabled();
            setPosition(75);
        });

        // Слайдер позиции
        positionSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                positionValueText.setText((int) value + "%");
            }
        });

        positionSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(Slider slider) {
                isSliderBeingDragged = true;
            }

            @Override
            public void onStopTrackingTouch(Slider slider) {
                isSliderBeingDragged = false;
                setPosition((int) slider.getValue());
            }
        });

        // FAB для настроек
        fab.setOnClickListener(v -> {
            vibrateIfEnabled();
            openSettingsActivity();
        });
    }

    /**
     * Открыть экран настроек
     */
    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Вибрация при нажатии (если включена)
     */
    private void vibrateIfEnabled() {
        if (preferencesManager.isVibrationEnabled() && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(50); // 50 мс
        }
    }

    /**
     * Отправка команды на сервер
     */
    private void sendCommand(String action) {
        String curtainId = preferencesManager.getDefaultCurtainId();
        CurtainCommand command = new CurtainCommand(curtainId, action);

        apiClient.sendCommand(command, new CurtainApiClient.ApiCallback<ServerResponse>() {
            @Override
            public void onSuccess(ServerResponse result) {
                if (result.isSuccess()) {
                    Toast.makeText(MainActivity.this, "Команда отправлена", Toast.LENGTH_SHORT).show();
                    if (result.getCurtainState() != null) {
                        updateUI(result.getCurtainState());
                    }
                    // Обновить состояние через секунду
                    new Handler().postDelayed(() -> refreshCurtainState(), 1000);
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Установка позиции штор
     */
    private void setPosition(int position) {
        String curtainId = preferencesManager.getDefaultCurtainId();
        CurtainCommand command = new CurtainCommand(curtainId, "set_position", position);

        apiClient.sendCommand(command, new CurtainApiClient.ApiCallback<ServerResponse>() {
            @Override
            public void onSuccess(ServerResponse result) {
                if (result.isSuccess()) {
                    Toast.makeText(MainActivity.this, "Позиция установлена: " + position + "%", Toast.LENGTH_SHORT).show();
                    if (result.getCurtainState() != null) {
                        updateUI(result.getCurtainState());
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка: " + result.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Ошибка: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Обновление состояния штор
     */
    private void refreshCurtainState() {
        String curtainId = preferencesManager.getDefaultCurtainId();

        apiClient.getCurtainState(curtainId, new CurtainApiClient.ApiCallback<CurtainState>() {
            @Override
            public void onSuccess(CurtainState result) {
                updateUI(result);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, "Ошибка обновления: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Проверка соединения с сервером
     */
    private void checkConnection() {
        connectionStatusText.setText("Проверка соединения...");

        apiClient.checkConnection(new CurtainApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isConnected) {
                if (isConnected) {
                    connectionStatusIcon.setImageResource(android.R.drawable.presence_online);
                    connectionStatusText.setText("Подключено");
                    connectionStatusText.setTextColor(getResources().getColor(R.color.status_connected, null));
                    refreshCurtainState();
                } else {
                    connectionStatusIcon.setImageResource(android.R.drawable.presence_offline);
                    connectionStatusText.setText("Не подключено");
                    connectionStatusText.setTextColor(getResources().getColor(R.color.status_disconnected, null));
                }
            }

            @Override
            public void onError(String error) {
                connectionStatusIcon.setImageResource(android.R.drawable.presence_offline);
                connectionStatusText.setText("Ошибка соединения");
                connectionStatusText.setTextColor(getResources().getColor(R.color.status_disconnected, null));
            }
        });
    }

    /**
     * Обновление UI с новым состоянием
     */
    private void updateUI(CurtainState state) {
        currentState = state;

        curtainNameText.setText(state.getName());
        curtainStatusText.setText("Статус: " + state.getStatusInRussian());
        positionValueText.setText(state.getPosition() + "%");

        if (!isSliderBeingDragged) {
            positionSlider.setValue(state.getPosition());
        }

        // Обновление времени
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateText.setText("Последнее обновление: " + sdf.format(new Date(state.getLastUpdate())));

        // Изменение цвета статуса
        if (state.isMoving()) {
            curtainStatusText.setTextColor(getResources().getColor(R.color.status_moving, null));
        } else {
            curtainStatusText.setTextColor(getResources().getColor(R.color.status_connected, null));
        }
    }



    /**
     * Запуск автоматического обновления
     */
    private void startAutoUpdate() {
        updateHandler = new Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                refreshCurtainState();
                int interval = preferencesManager.getAutoUpdateInterval();
                updateHandler.postDelayed(this, interval);
            }
        };
        int interval = preferencesManager.getAutoUpdateInterval();
        updateHandler.postDelayed(updateRunnable, interval);
    }

    /**
     * Остановка автоматического обновления
     */
    private void stopAutoUpdate() {
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Перезагрузить настройки при возврате из настроек
        reloadSettings();
        startAutoUpdate();
    }

    /**
     * Перезагрузка настроек
     */
    private void reloadSettings() {
        String serverUrl = preferencesManager.getServerUrl();
        apiClient.setServerUrl(serverUrl);
        serverUrlText.setText("Сервер: " + serverUrl);
        currentState.setId(preferencesManager.getDefaultCurtainId());
        currentState.setName(preferencesManager.getCurtainName());
        curtainNameText.setText(currentState.getName());
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoUpdate();
        if (apiClient != null) {
            apiClient.shutdown();
        }
    }
}