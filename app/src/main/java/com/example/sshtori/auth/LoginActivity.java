package com.example.sshtori;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGuest;
    private TextView tvRegisterLink, tvForgotPassword;
    private ProgressBar progressBar;
    private LinearLayout loginForm;
    private SharedPreferences prefs;
    private TextInputLayout tilEmail, tilPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Инициализация
        initViews();
        setupToolbar();
        setupClickListeners();

        // Проверяем, есть ли сохраненная сессия
        checkSavedSession();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGuest = findViewById(R.id.btn_guest);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        progressBar = findViewById(R.id.progress_bar);
        loginForm = findViewById(R.id.login_form);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);

        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Вход в систему");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGuest.setOnClickListener(v -> continueAsGuest());
        tvRegisterLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void checkSavedSession() {
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        String savedEmail = prefs.getString("userEmail", "");

        if (isLoggedIn && !savedEmail.isEmpty()) {
            // Автоматический вход
            navigateToMain();
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Валидация
        if (!validateInputs(email, password)) {
            return;
        }

        // Показываем прогресс
        showProgress(true);

        // Имитация проверки логина (замените на реальный API запрос)
        performLogin(email, password);
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            tilEmail.setError("Введите email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Неверный формат email");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        if (password.isEmpty()) {
            tilPassword.setError("Введите пароль");
            isValid = false;
        } else if (password.length() < 4) {
            tilPassword.setError("Пароль должен быть не менее 4 символов");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        return isValid;
    }

    private void performLogin(String email, String password) {
        // Имитация задержки сети
        new android.os.Handler().postDelayed(() -> {
            // Здесь должен быть реальный запрос к вашему серверу
            // Пример для демонстрации:
            if (isValidCredentials(email, password)) {
                saveUserSession(email);
                navigateToMain();
            } else {
                showProgress(false);
                Snackbar.make(findViewById(android.R.id.content),
                        "Неверный email или пароль", Snackbar.LENGTH_LONG).show();
            }
        }, 1500);
    }

    private boolean isValidCredentials(String email, String password) {
        // Здесь проверка в вашей базе данных или на сервере
        // Сейчас просто демо-пользователь
        SharedPreferences regPrefs = getSharedPreferences("RegisteredUsers", MODE_PRIVATE);
        String savedPassword = regPrefs.getString(email, "");
        return savedPassword.equals(password) && !savedPassword.isEmpty();
    }

    private void saveUserSession(String email) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userEmail", email);
        editor.putString("loginType", "registered");
        editor.apply();
    }

    private void continueAsGuest() {
        Snackbar.make(findViewById(android.R.id.content),
                        "Продолжить как гость? Некоторые функции могут быть ограничены",
                        Snackbar.LENGTH_LONG)
                .setAction("ДА", v -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("loginType", "guest");
                    editor.apply();
                    navigateToMain();
                })
                .show();
    }

    private void showForgotPasswordDialog() {
        // Диалог восстановления пароля
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Восстановление пароля");

        final EditText input = new EditText(this);
        input.setHint("Введите ваш email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Отправить", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                Toast.makeText(this, "Инструкция по восстановлению отправлена на " + email,
                        Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void showProgress(boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
        loginForm.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1);

        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.animate().setDuration(shortAnimTime).alpha(show ? 1 : 0);
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}