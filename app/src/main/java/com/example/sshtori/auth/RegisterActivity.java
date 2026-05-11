package com.example.sshtori.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.sshtori.R;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextInputLayout tilUsername, tilEmail, tilPassword, tilConfirmPassword;
    private SharedPreferences regPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupToolbar();
        setupClickListeners();

        regPrefs = getSharedPreferences("RegisteredUsers", MODE_PRIVATE);
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Регистрация");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegistration());
    }

    private void attemptRegistration() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!validateInputs(username, email, password, confirmPassword)) {
            return;
        }

        // Проверяем, не зарегистрирован ли уже пользователь
        if (regPrefs.contains(email)) {
            Toast.makeText(this, "Пользователь с таким email уже существует", Toast.LENGTH_LONG).show();
            return;
        }

        showProgress(true);

        // Имитация регистрации
        new android.os.Handler().postDelayed(() -> {
            registerUser(username, email, password);
        }, 1500);
    }

    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (username.isEmpty()) {
            tilUsername.setError("Введите имя пользователя");
            isValid = false;
        } else if (username.length() < 3) {
            tilUsername.setError("Имя должно быть не менее 3 символов");
            isValid = false;
        } else {
            tilUsername.setError(null);
        }

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

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Подтвердите пароль");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Пароли не совпадают");
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        return isValid;
    }

    private void registerUser(String username, String email, String password) {
        // Сохраняем пользователя (в реальном приложении - отправка на сервер)
        SharedPreferences.Editor editor = regPrefs.edit();
        editor.putString(email, password);
        editor.putString(email + "_username", username);
        editor.apply();

        showProgress(false);

        Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_LONG).show();

        // Возвращаемся на экран входа
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private void showProgress(boolean show) {
        btnRegister.setEnabled(!show);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            btnRegister.setText("");
        } else {
            btnRegister.setText("Зарегистрироваться");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}