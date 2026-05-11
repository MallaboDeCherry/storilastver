package com.example.sshtori.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.sshtori.models.CurtainState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Клиент для прямого взаимодействия с ESP32
 * Отправляет HTTP GET команды напрямую на контроллер
 */
public class CurtainApiClient {
    private static final String TAG = "CurtainApiClient";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 500;

    private String esp32BaseUrl;  // http://192.168.1.100:80 или http://smart-curtain.local
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private ConnectionListener connectionListener;
    private int connectionTimeout = 3000; // ms
    private boolean isReconnecting = false;

    /**
     * Слушатель состояния соединения
     */
    public interface ConnectionListener {
        void onConnected();
        void onDisconnected();
        void onConnectionError(String error);
    }

    /**
     * Интерфейс для обратных вызовов API
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    public CurtainApiClient(String esp32Ip, int port) {
        this.esp32BaseUrl = "http://" + esp32Ip + ":" + port;
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "ESP32 API Client initialized with URL: " + esp32BaseUrl);
    }

    public CurtainApiClient(String esp32Hostname) {
        this.esp32BaseUrl = "http://" + esp32Hostname;
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "ESP32 API Client initialized with hostname: " + esp32BaseUrl);
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void setConnectionTimeout(int timeoutMs) {
        this.connectionTimeout = timeoutMs;
    }

    public void updateEsp32Address(String ip, int port) {
        this.esp32BaseUrl = "http://" + ip + ":" + port;
        Log.d(TAG, "ESP32 address updated to: " + esp32BaseUrl);
    }

    public void updateEsp32Hostname(String hostname) {
        this.esp32BaseUrl = "http://" + hostname;
        Log.d(TAG, "ESP32 hostname updated to: " + esp32BaseUrl);
    }

    public String getEsp32BaseUrl() {
        return esp32BaseUrl;
    }

    /**
     * Базовый GET запрос с поддержкой retry
     */
    private String executeGetRequest(String endpoint, int retryCount) throws IOException {
        if (retryCount > MAX_RETRIES) {
            throw new IOException("Max retries exceeded for endpoint: " + endpoint);
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(esp32BaseUrl + endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectionTimeout);
            connection.setReadTimeout(connectionTimeout);
            connection.setRequestProperty("User-Agent", "SmartCurtain-Android/1.0");

            int responseCode = connection.getResponseCode();
            Log.d(TAG, String.format("GET %s -> %d (retry %d)", endpoint, responseCode, retryCount));

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Уведомляем об успешном подключении
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnected());
                }

                return response.toString();
            } else {
                // Читаем ошибку если есть
                String errorResponse = "";
                try {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream()));
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        error.append(line);
                    }
                    errorReader.close();
                    errorResponse = error.toString();
                } catch (Exception ignored) {}

                throw new IOException("HTTP " + responseCode + ": " + errorResponse);
            }

        } catch (SocketTimeoutException e) {
            Log.w(TAG, "Timeout on endpoint: " + endpoint + ", retry " + (retryCount + 1));
            if (connectionListener != null && retryCount == 0) {
                mainHandler.post(() -> connectionListener.onConnectionError("Timeout: " + e.getMessage()));
            }
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return executeGetRequest(endpoint, retryCount + 1);

        } catch (UnknownHostException e) {
            Log.e(TAG, "Unknown host: " + esp32BaseUrl);
            if (connectionListener != null) {
                mainHandler.post(() -> connectionListener.onDisconnected());
            }
            throw e;

        } catch (IOException e) {
            Log.e(TAG, "IO Error on endpoint: " + endpoint, e);
            if (retryCount < MAX_RETRIES - 1) {
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return executeGetRequest(endpoint, retryCount + 1);
            }
            throw e;

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Проверка соединения с ESP32
     */
    public void checkConnection(ApiCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/ping", 0);
                boolean connected = response != null &&
                        (response.contains("pong") || response.contains("OK"));
                mainHandler.post(() -> callback.onSuccess(connected));
            } catch (IOException e) {
                Log.e(TAG, "Connection check failed", e);
                mainHandler.post(() -> callback.onSuccess(false));
            }
        });
    }

    /**
     * Открыть штору
     */
    public void openCurtain(ApiCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/open", 0);
                Log.i(TAG, "Open command sent, response: " + response);
                mainHandler.post(() -> callback.onSuccess(true));
            } catch (IOException e) {
                Log.e(TAG, "Failed to open curtain", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Закрыть штору
     */
    public void closeCurtain(ApiCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/close", 0);
                Log.i(TAG, "Close command sent, response: " + response);
                mainHandler.post(() -> callback.onSuccess(true));
            } catch (IOException e) {
                Log.e(TAG, "Failed to close curtain", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Остановить штору
     */
    public void stopCurtain(ApiCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/stop", 0);
                Log.i(TAG, "Stop command sent, response: " + response);
                mainHandler.post(() -> callback.onSuccess(true));
            } catch (IOException e) {
                Log.e(TAG, "Failed to stop curtain", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Установить позицию (0-100%)
     */
    public void setPosition(int position, ApiCallback<Boolean> callback) {
        if (position < 0 || position > 100) {
            mainHandler.post(() -> callback.onError("Invalid position: " + position));
            return;
        }

        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/set?pos=" + position, 0);
                Log.i(TAG, "Set position " + position + " command sent, response: " + response);
                mainHandler.post(() -> callback.onSuccess(true));
            } catch (IOException e) {
                Log.e(TAG, "Failed to set position", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Получить состояние шторы
     */
    public void getCurtainState(ApiCallback<CurtainState> callback) {
        executorService.execute(() -> {
            try {
                // Пробуем получить статус
                String response = executeGetRequest("/status", 0);
                Log.d(TAG, "Status response: " + response);

                CurtainState state = CurtainState.parseFromEsp32Response(response);
                mainHandler.post(() -> callback.onSuccess(state));

            } catch (IOException e) {
                Log.e(TAG, "Failed to get curtain state", e);
                CurtainState errorState = new CurtainState();
                errorState.setErrorMessage(e.getMessage());
                mainHandler.post(() -> callback.onSuccess(errorState));
            }
        });
    }

    /**
     * Получить информацию об устройстве
     */
    public void getDeviceInfo(ApiCallback<String> callback) {
        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/info", 0);
                mainHandler.post(() -> callback.onSuccess(response));
            } catch (IOException e) {
                Log.e(TAG, "Failed to get device info", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Отправить команду калибровки
     */
    public void calibrate(ApiCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/calibrate", 0);
                Log.i(TAG, "Calibrate command sent, response: " + response);
                mainHandler.post(() -> callback.onSuccess(true));
            } catch (IOException e) {
                Log.e(TAG, "Failed to calibrate", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Асинхронный метод для отправки любой команды
     */
    public void sendCommand(String command, ApiCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                String response = executeGetRequest("/" + command, 0);
                mainHandler.post(() -> callback.onSuccess(true));
            } catch (IOException e) {
                Log.e(TAG, "Failed to send command: " + command, e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Попытка переподключения
     */
    /**
     * Попытка переподключения
     */
    public void reconnect() {
        if (isReconnecting) return;

        isReconnecting = true;
        executorService.execute(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
                String response = executeGetRequest("/ping", 0);
                boolean connected = response != null &&
                        (response.contains("pong") || response.contains("OK"));
                if (connected && connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnected());
                } else if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onDisconnected());
                }
            } catch (Exception e) {
                Log.e(TAG, "Reconnect failed", e);
                if (connectionListener != null) {
                    mainHandler.post(() -> connectionListener.onConnectionError(e.getMessage()));
                }
            } finally {
                isReconnecting = false;
            }
        });
    }

    /**
     * Освобождение ресурсов
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}