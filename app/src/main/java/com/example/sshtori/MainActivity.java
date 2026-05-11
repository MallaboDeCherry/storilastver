package com.example.sshtori;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sshtori.controllers.CurtainController;
import com.example.sshtori.models.CurtainState;
import com.example.sshtori.network.Esp32NetworkManager;
import com.example.sshtori.utils.PreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.sshtori.auth.LoginActivity;

public class MainActivity extends AppCompatActivity {

    // UI элементы
    private ImageView connectionStatusIcon;
    private TextView connectionStatusText;
    private TextView curtainNameText;
    private TextView curtainStatusText;
    private TextView positionValueText;
    private TextView infoText;
    private TextView lastUpdateText;
    private Slider positionSlider;
    private MaterialButton btnOpen, btnClose, btnStop, btnRefresh;
    private MaterialButton btnQuick25, btnQuick50, btnQuick75;
    private FloatingActionButton fab;

    private PreferencesManager preferencesManager;
    private CurtainState currentState;
    private Vibrator vibrator;
    private boolean isSliderBeingDragged = false;

    // Контроллер для управления ESP32
    private CurtainController curtainController;
    private ExecutorService executorService;
    private Handler mainHandler;
    private Handler autoUpdateHandler;
    private Runnable autoUpdateRunnable;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Проверка авторизации
        SharedPreferences loginPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        if (!loginPrefs.getBoolean("isLoggedIn", false)) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        initializeViews();
        initializeController();
        setupListeners();

        // Показываем приветствие
        showWelcomeMessage(loginPrefs);

        // Проверяем соединение с ESP32
        checkConnection();

        // Запускаем автообновление
        startAutoUpdate();
    }

    private void initializeViews() {
        connectionStatusIcon = findViewById(R.id.connectionStatusIcon);
        connectionStatusText = findViewById(R.id.connectionStatusText);
        curtainNameText = findViewById(R.id.curtainNameText);
        curtainStatusText = findViewById(R.id.curtainStatusText);
        positionValueText = findViewById(R.id.positionValueText);
        infoText = findViewById(R.id.serverUrlText);
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

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        preferencesManager = new PreferencesManager(this);

        // Начальное состояние UI
        positionSlider.setEnabled(false);
        connectionStatusText.setText("Инициализация...");
    }

    private void initializeController() {
        String esp32Ip = preferencesManager.getEsp32Ip();
        int esp32Port = preferencesManager.getEsp32Port();
        boolean useMdns = preferencesManager.isUseMdns();

        if (useMdns) {
            String mdnsHostname = preferencesManager.getMdnsHostname();
            curtainController = new Esp32NetworkManager(mdnsHostname);
            infoText.setText("ESP32: " + mdnsHostname);
        } else {
            curtainController = new Esp32NetworkManager(esp32Ip, esp32Port);
            infoText.setText("ESP32: " + esp32Ip + ":" + esp32Port);
        }

        currentState = new CurtainState();
        currentState.setDeviceId(preferencesManager.getDefaultCurtainId());
        currentState.setDeviceName(preferencesManager.getCurtainName());
        curtainNameText.setText(currentState.getDeviceName());
    }

    private void setupListeners() {
        btnOpen.setOnClickListener(v -> {
            vibrateIfEnabled();
            sendOpenCommand();
        });

        btnClose.setOnClickListener(v -> {
            vibrateIfEnabled();
            sendCloseCommand();
        });

        btnStop.setOnClickListener(v -> {
            vibrateIfEnabled();
            sendStopCommand();
        });

        btnRefresh.setOnClickListener(v -> {
            vibrateIfEnabled();
            refreshCurtainState();
        });

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

        fab.setOnClickListener(v -> {
            vibrateIfEnabled();
            openSettingsActivity();
        });
    }

    private void showWelcomeMessage(SharedPreferences loginPrefs) {
        String userEmail = loginPrefs.getString("userEmail", "");
        String loginType = loginPrefs.getString("loginType", "guest");

        String welcomeMessage;
        if (loginType.equals("guest")) {
            welcomeMessage = "Добро пожаловать, Гость!";
        } else {
            String userName = userEmail.contains("@") ? userEmail.split("@")[0] : userEmail;
            welcomeMessage = "С возвращением, " + userName + "!";
        }
        Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();
    }

    private void sendOpenCommand() {
        if (curtainController == null) return;

        curtainController.openCurtain(new CurtainController.CurtainCallback() {
            @Override
            public void onSuccess(Object result) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Открытие...", Toast.LENGTH_SHORT).show();
                    refreshCurtainState();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void sendCloseCommand() {
        if (curtainController == null) return;

        curtainController.closeCurtain(new CurtainController.CurtainCallback() {
            @Override
            public void onSuccess(Object result) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Закрытие...", Toast.LENGTH_SHORT).show();
                    refreshCurtainState();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void sendStopCommand() {
        if (curtainController == null) return;

        curtainController.stopCurtain(new CurtainController.CurtainCallback() {
            @Override
            public void onSuccess(Object result) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Остановлено", Toast.LENGTH_SHORT).show();
                    refreshCurtainState();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() ->
                        Toast.makeText(MainActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void setPosition(int position) {
        if (curtainController == null) return;

        curtainController.setPosition(position, new CurtainController.CurtainCallback() {
            @Override
            public void onSuccess(Object result) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Позиция " + position + "%", Toast.LENGTH_SHORT).show();
                    refreshCurtainState();
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
                    if (!isSliderBeingDragged && currentState != null) {
                        positionSlider.setValue(currentState.getPosition());
                        positionValueText.setText(currentState.getPosition() + "%");
                    }
                });
            }
        });
    }

    private void refreshCurtainState() {
        if (isUpdating || curtainController == null) return;

        isUpdating = true;

        curtainController.getState(new CurtainController.CurtainCallback() {
            @Override
            public void onSuccess(Object result) {
                mainHandler.post(() -> {
                    if (result instanceof CurtainState) {
                        updateUI((CurtainState) result);
                    }
                    isUpdating = false;
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    isUpdating = false;
                });
            }
        });
    }

    private void checkConnection() {
        if (curtainController == null) return;

        curtainController.checkConnection(new CurtainController.CurtainCallback() {
            @Override
            public void onSuccess(Object result) {
                mainHandler.post(() -> {
                    boolean connected = result instanceof Boolean && (Boolean) result;
                    if (connected) {
                        connectionStatusIcon.setImageResource(android.R.drawable.presence_online);
                        connectionStatusText.setText("Подключено");
                        connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                        positionSlider.setEnabled(true);
                        refreshCurtainState();
                    } else {
                        connectionStatusIcon.setImageResource(android.R.drawable.presence_offline);
                        connectionStatusText.setText("ESP32 не найден");
                        connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        positionSlider.setEnabled(false);

                        Snackbar.make(findViewById(android.R.id.content),
                                        "ESP32 не найден. Проверьте настройки.", Snackbar.LENGTH_LONG)
                                .setAction("Настройки", v -> openSettingsActivity())
                                .show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                    connectionStatusIcon.setImageResource(android.R.drawable.presence_offline);
                    connectionStatusText.setText("Ошибка: " + error);
                    connectionStatusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                });
            }
        });
    }

    private void updateUI(CurtainState state) {
        if (state == null) return;

        currentState = state;

        curtainNameText.setText(preferencesManager.getCurtainName());
        curtainStatusText.setText("Статус: " + state.getFormattedStatus());

        int position = state.getPosition();
        if (position >= 0) {
            positionValueText.setText(position + "%");
            if (!isSliderBeingDragged) {
                positionSlider.setValue(position);
            }
        } else {
            positionValueText.setText("N/A");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        lastUpdateText.setText("Обновлено: " + sdf.format(new Date(state.getLastUpdate())));

        // Блокируем слайдер при движении
        positionSlider.setEnabled(!state.isMoving() && connectionStatusText.getText().toString().equals("Подключено"));

        // Обновляем цвета
        if (state.isMoving()) {
            curtainStatusText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        } else if (state.isOpened()) {
            curtainStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (state.isClosed()) {
            curtainStatusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            curtainStatusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }
    }

    private void startAutoUpdate() {
        if (autoUpdateHandler == null) {
            autoUpdateHandler = new Handler(Looper.getMainLooper());
        }

        if (autoUpdateRunnable != null) {
            autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
        }

        autoUpdateRunnable = () -> {
            refreshCurtainState();
            int interval = preferencesManager.getAutoUpdateInterval();
            if (interval > 0) {
                autoUpdateHandler.postDelayed(autoUpdateRunnable, interval);
            }
        };

        int interval = preferencesManager.getAutoUpdateInterval();
        autoUpdateHandler.postDelayed(autoUpdateRunnable, interval);
    }

    private void stopAutoUpdate() {
        if (autoUpdateHandler != null && autoUpdateRunnable != null) {
            autoUpdateHandler.removeCallbacks(autoUpdateRunnable);
        }
    }

    private void vibrateIfEnabled() {
        if (preferencesManager.isVibrationEnabled() && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(50);
        }
    }

    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        } else if (id == R.id.action_account) {
            showAccountInfo();
            return true;
        } else if (id == R.id.action_calibrate) {
            showCalibrateDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCalibrateDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Калибровка штор")
                .setMessage("Калибровка позволит определить крайние положения шторы.\n\nПродолжить?")
                .setPositiveButton("Начать", (dialog, which) -> sendCalibrateCommand())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void sendCalibrateCommand() {
        if (curtainController == null) return;

        vibrateIfEnabled();
        Toast.makeText(this, "Начинаем калибровку...", Toast.LENGTH_LONG).show();

        // TODO: Добавить метод calibrate в интерфейс CurtainController и Esp32NetworkManager
        // Пока показываем сообщение
        Toast.makeText(this, "Функция калибровки будет добавлена", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        SharedPreferences loginPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.clear();
        editor.apply();

        stopAutoUpdate();

        if (curtainController != null) {
            curtainController.shutdown();
        }

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showAccountInfo() {
        SharedPreferences loginPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userEmail = loginPrefs.getString("userEmail", "");
        String loginType = loginPrefs.getString("loginType", "guest");

        String message = loginType.equals("guest")
                ? "Вы вошли как гость\n\nESP32: " + infoText.getText()
                : "Email: " + userEmail + "\n\nESP32: " + infoText.getText() + "\n\nШторы: " + preferencesManager.getCurtainName();

        new AlertDialog.Builder(this)
                .setTitle("Информация")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (curtainController != null) {
            // Обновляем настройки клиента
            String esp32Ip = preferencesManager.getEsp32Ip();
            int esp32Port = preferencesManager.getEsp32Port();
            boolean useMdns = preferencesManager.isUseMdns();

            if (curtainController instanceof Esp32NetworkManager) {
                if (useMdns) {
                    ((Esp32NetworkManager) curtainController).updateAddress(preferencesManager.getMdnsHostname());
                } else {
                    ((Esp32NetworkManager) curtainController).updateAddress(esp32Ip, esp32Port);
                }
            }
        }
        startAutoUpdate();
        checkConnection();
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
        if (curtainController != null) {
            curtainController.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}