package com.example.sshtori;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sshtori.network.CurtainApiClient;
import com.example.sshtori.utils.PreferencesManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Экран настроек приложения
 */
public class SettingsActivity extends AppCompatActivity {

    // UI элементы
    private TextInputEditText serverUrlInput;
    private TextInputEditText curtainNameInput;
    private TextInputEditText curtainIdInput;
    private TextInputEditText nodeMcuIpInput;
    private TextInputEditText nodeMcuPortInput;
    private TextInputEditText autoUpdateIntervalInput;
    private TextInputEditText connectionTimeoutInput;
    private SwitchMaterial vibrationSwitch;
    private SwitchMaterial soundSwitch;
    private MaterialButton btnSave;
    private MaterialButton btnTestConnection;
    private MaterialButton btnResetDefaults;
    private MaterialButton btnExportSettings;

    private PreferencesManager preferencesManager;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Включить кнопку "Назад" в ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Настройки");
        }

        initializeViews();
        preferencesManager = new PreferencesManager(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        loadSettings();
        setupListeners();
        setupBackPressHandler();
    }

    /**
     * Инициализация UI элементов
     */
    private void initializeViews() {
        serverUrlInput = findViewById(R.id.serverUrlInput);
        curtainNameInput = findViewById(R.id.curtainNameInput);
        curtainIdInput = findViewById(R.id.curtainIdInput);
        nodeMcuIpInput = findViewById(R.id.nodeMcuIpInput);
        nodeMcuPortInput = findViewById(R.id.nodeMcuPortInput);
        autoUpdateIntervalInput = findViewById(R.id.autoUpdateIntervalInput);
        connectionTimeoutInput = findViewById(R.id.connectionTimeoutInput);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        soundSwitch = findViewById(R.id.soundSwitch);
        btnSave = findViewById(R.id.btnSave);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnResetDefaults = findViewById(R.id.btnResetDefaults);
        btnExportSettings = findViewById(R.id.btnExportSettings);
    }

    /**
     * Загрузка текущих настроек
     */
    private void loadSettings() {
        serverUrlInput.setText(preferencesManager.getServerUrl());
        curtainNameInput.setText(preferencesManager.getCurtainName());
        curtainIdInput.setText(preferencesManager.getDefaultCurtainId());
        nodeMcuIpInput.setText(preferencesManager.getNodeMcuIp());
        nodeMcuPortInput.setText(String.valueOf(preferencesManager.getNodeMcuPort()));
        autoUpdateIntervalInput.setText(String.valueOf(preferencesManager.getAutoUpdateInterval()));
        connectionTimeoutInput.setText(String.valueOf(preferencesManager.getConnectionTimeout()));
        vibrationSwitch.setChecked(preferencesManager.isVibrationEnabled());
        soundSwitch.setChecked(preferencesManager.isSoundEnabled());
    }

    /**
     * Настройка слушателей событий
     */
    private void setupListeners() {
        btnSave.setOnClickListener(v -> {
            vibrateIfEnabled();
            saveSettings();
        });

        btnTestConnection.setOnClickListener(v -> {
            vibrateIfEnabled();
            testConnection();
        });

        btnResetDefaults.setOnClickListener(v -> {
            vibrateIfEnabled();
            showResetConfirmationDialog();
        });

        btnExportSettings.setOnClickListener(v -> {
            vibrateIfEnabled();
            exportSettings();
        });
    }

    /**
     * Сохранение настроек
     */
    private void saveSettings() {
        try {
            // Валидация полей
            String serverUrl = serverUrlInput.getText().toString().trim();
            String curtainName = curtainNameInput.getText().toString().trim();
            String curtainId = curtainIdInput.getText().toString().trim();
            String nodeMcuIp = nodeMcuIpInput.getText().toString().trim();
            String nodeMcuPortStr = nodeMcuPortInput.getText().toString().trim();
            String autoUpdateStr = autoUpdateIntervalInput.getText().toString().trim();
            String timeoutStr = connectionTimeoutInput.getText().toString().trim();

            // Проверка обязательных полей
            if (serverUrl.isEmpty() || curtainId.isEmpty() || curtainName.isEmpty()) {
                Toast.makeText(this, "❌ Заполните все обязательные поля", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверка URL
            if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
                Toast.makeText(this, "❌ URL должен начинаться с http:// или https://", Toast.LENGTH_SHORT).show();
                return;
            }

            // Парсинг числовых значений
            int nodeMcuPort = Integer.parseInt(nodeMcuPortStr);
            int autoUpdateInterval = Integer.parseInt(autoUpdateStr);
            int connectionTimeout = Integer.parseInt(timeoutStr);

            // Валидация диапазонов
            if (nodeMcuPort < 1 || nodeMcuPort > 65535) {
                Toast.makeText(this, "❌ Порт должен быть от 1 до 65535", Toast.LENGTH_SHORT).show();
                return;
            }

            if (autoUpdateInterval < 1000) {
                Toast.makeText(this, "❌ Интервал обновления должен быть минимум 1000 мс", Toast.LENGTH_SHORT).show();
                return;
            }

            if (connectionTimeout < 1000) {
                Toast.makeText(this, "❌ Таймаут должен быть минимум 1000 мс", Toast.LENGTH_SHORT).show();
                return;
            }

            // Сохранение настроек
            preferencesManager.setServerUrl(serverUrl);
            preferencesManager.setCurtainName(curtainName);
            preferencesManager.setDefaultCurtainId(curtainId);
            preferencesManager.setNodeMcuIp(nodeMcuIp);
            preferencesManager.setNodeMcuPort(nodeMcuPort);
            preferencesManager.setAutoUpdateInterval(autoUpdateInterval);
            preferencesManager.setConnectionTimeout(connectionTimeout);
            preferencesManager.setVibrationEnabled(vibrationSwitch.isChecked());
            preferencesManager.setSoundEnabled(soundSwitch.isChecked());

            Toast.makeText(this, "✅ Настройки сохранены успешно!", Toast.LENGTH_SHORT).show();

            // Возврат на главный экран
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "❌ Ошибка: проверьте числовые значения", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Проверка соединения с сервером
     */
    private void testConnection() {
        String serverUrl = serverUrlInput.getText().toString().trim();

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "❌ Введите URL сервера", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "🔄 Проверка соединения...", Toast.LENGTH_SHORT).show();

        CurtainApiClient apiClient = new CurtainApiClient(serverUrl);
        apiClient.checkConnection(new CurtainApiClient.ApiCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isConnected) {
                if (isConnected) {
                    Toast.makeText(SettingsActivity.this, "✅ Соединение успешно!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "❌ Сервер не отвечает", Toast.LENGTH_LONG).show();
                }
                apiClient.shutdown();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SettingsActivity.this, "❌ Ошибка: " + error, Toast.LENGTH_LONG).show();
                apiClient.shutdown();
            }
        });
    }

    /**
     * Показать диалог подтверждения сброса
     */
    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🔄 Сброс настроек")
                .setMessage("Вы уверены, что хотите сбросить все настройки к значениям по умолчанию?")
                .setPositiveButton("Да, сбросить", (dialog, which) -> {
                    resetToDefaults();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /**
     * Сброс к настройкам по умолчанию
     */
    private void resetToDefaults() {
        preferencesManager.resetToDefaults();
        loadSettings();
        Toast.makeText(this, "✅ Настройки сброшены к значениям по умолчанию", Toast.LENGTH_SHORT).show();
    }

    /**
     * Экспорт настроек в буфер обмена
     */
    private void exportSettings() {
        String settings = preferencesManager.exportSettings();

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Настройки приложения", settings);
        clipboard.setPrimaryClip(clip);

        // Показать диалог с настройками
        new AlertDialog.Builder(this)
                .setTitle("📤 Экспорт настроек")
                .setMessage(settings + "\n\n✅ Настройки скопированы в буфер обмена!")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Вибрация при нажатии (если включена)
     */
    private void vibrateIfEnabled() {
        if (vibrationSwitch.isChecked() && vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(50); // 50 мс
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Обработка нажатия кнопки "Назад" в ActionBar
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Настройка обработчика кнопки "Назад"
     */
    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Предупреждение о несохраненных изменениях
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("⚠️ Выход")
                        .setMessage("Вы хотите выйти без сохранения изменений?")
                        .setPositiveButton("Выйти", (dialog, which) -> finish())
                        .setNegativeButton("Остаться", null)
                        .show();
            }
        });
    }
}
