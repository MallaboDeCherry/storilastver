package com.example.sshtori.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.sshtori.controllers.CurtainController;
import com.example.sshtori.models.CurtainState;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Esp32NetworkManager implements CurtainController {

    private static final String TAG = "Esp32NetworkManager";
    private static final int CONNECTION_TIMEOUT = 3000;
    private static final int MAX_RETRIES = 2;

    private String esp32BaseUrl;
    private ExecutorService executorService;
    private Handler mainHandler;

    public Esp32NetworkManager(String ip, int port) {
        this.esp32BaseUrl = "http://" + ip + ":" + port;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Initialized with URL: " + esp32BaseUrl);
    }

    public Esp32NetworkManager(String hostname) {
        this.esp32BaseUrl = "http://" + hostname;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Initialized with hostname: " + esp32BaseUrl);
    }

    public void updateAddress(String ip, int port) {
        this.esp32BaseUrl = "http://" + ip + ":" + port;
        Log.d(TAG, "Address updated to: " + esp32BaseUrl);
    }

    public void updateAddress(String hostname) {
        this.esp32BaseUrl = "http://" + hostname;
        Log.d(TAG, "Hostname updated to: " + esp32BaseUrl);
    }

    private void executeGetRequest(String endpoint, CurtainCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(esp32BaseUrl + endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(CONNECTION_TIMEOUT);

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "GET " + endpoint + " -> " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    mainHandler.post(() -> callback.onSuccess(true));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP Error: " + responseCode));
                }
            } catch (IOException e) {
                Log.e(TAG, "Request failed: " + endpoint, e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void executeGetRequestWithResponse(String endpoint, CurtainCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(esp32BaseUrl + endpoint);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(CONNECTION_TIMEOUT);

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    CurtainState state = CurtainState.parseFromEsp32Response(response.toString());
                    mainHandler.post(() -> callback.onSuccess(state));
                } else {
                    mainHandler.post(() -> callback.onError("HTTP Error: " + responseCode));
                }
            } catch (IOException e) {
                Log.e(TAG, "Request failed: " + endpoint, e);
                CurtainState errorState = new CurtainState();
                errorState.setErrorMessage(e.getMessage());
                mainHandler.post(() -> callback.onSuccess(errorState));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Override
    public void openCurtain(CurtainCallback callback) {
        executeGetRequest("/open", callback);
    }

    @Override
    public void closeCurtain(CurtainCallback callback) {
        executeGetRequest("/close", callback);
    }

    @Override
    public void stopCurtain(CurtainCallback callback) {
        executeGetRequest("/stop", callback);
    }

    @Override
    public void setPosition(int position, CurtainCallback callback) {
        if (position < 0 || position > 100) {
            mainHandler.post(() -> callback.onError("Invalid position: " + position));
            return;
        }
        executeGetRequest("/set?pos=" + position, callback);
    }

    @Override
    public void getState(CurtainCallback callback) {
        executeGetRequestWithResponse("/status", callback);
    }

    @Override
    public void checkConnection(CurtainCallback callback) {
        executorService.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(esp32BaseUrl + "/ping");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECTION_TIMEOUT);
                connection.setReadTimeout(CONNECTION_TIMEOUT);

                int responseCode = connection.getResponseCode();
                boolean connected = (responseCode == HttpURLConnection.HTTP_OK);
                mainHandler.post(() -> callback.onSuccess(connected));
            } catch (IOException e) {
                Log.e(TAG, "Connection check failed", e);
                mainHandler.post(() -> callback.onSuccess(false));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}