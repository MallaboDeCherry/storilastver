package com.example.sshtori.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.sshtori.models.CurtainState;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Esp32ApiClient {
    private static final String TAG = "Esp32ApiClient";
    private String baseUrl;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private boolean isShuttingDown = false;  // Флаг для отслеживания состояния

    public Esp32ApiClient(String ip, int port) {
        this.baseUrl = "http://" + ip + ":" + port;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Created: " + baseUrl);
    }

    public Esp32ApiClient(String hostname) {
        this.baseUrl = "http://" + hostname;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "Created with mDNS: " + baseUrl);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface StatusCallback {
        void onSuccess(CurtainState state);
        void onError(String error);
    }

    public void ping(SimpleCallback callback) {
        if (isShuttingDown || executor.isShutdown()) {
            mainHandler.post(() -> callback.onError("Client is shutting down"));
            return;
        }
        executeGet("/ping", response -> {
            if ("pong".equals(response)) callback.onSuccess();
            else callback.onError("No response");
        }, callback::onError);
    }

    public void open(SimpleCallback callback) {
        if (isShuttingDown || executor.isShutdown()) {
            mainHandler.post(() -> callback.onError("Client is shutting down"));
            return;
        }
        executeGet("/open", response -> callback.onSuccess(), callback::onError);
    }

    public void close(SimpleCallback callback) {
        if (isShuttingDown || executor.isShutdown()) {
            mainHandler.post(() -> callback.onError("Client is shutting down"));
            return;
        }
        executeGet("/close", response -> callback.onSuccess(), callback::onError);
    }

    public void stop(SimpleCallback callback) {
        if (isShuttingDown || executor.isShutdown()) {
            mainHandler.post(() -> callback.onError("Client is shutting down"));
            return;
        }
        executeGet("/stop", response -> callback.onSuccess(), callback::onError);
    }

    public void setPosition(int position, SimpleCallback callback) {
        if (isShuttingDown || executor.isShutdown()) {
            mainHandler.post(() -> callback.onError("Client is shutting down"));
            return;
        }
        position = Math.max(0, Math.min(100, position));
        executeGet("/set?pos=" + position, response -> callback.onSuccess(), callback::onError);
    }

    public void getStatus(StatusCallback callback) {
        if (isShuttingDown || executor.isShutdown()) {
            mainHandler.post(() -> callback.onError("Client is shutting down"));
            return;
        }
        executeGet("/status", response -> {
            CurtainState state = CurtainState.parseFromEsp32Response(response);
            callback.onSuccess(state);
        }, callback::onError);
    }

    private void executeGet(String endpoint, ResponseCallback responseCallback, ErrorCallback errorCallback) {
        // Проверяем, не завершается ли работа клиента
        if (isShuttingDown || executor.isShutdown() || executor.isTerminated()) {
            mainHandler.post(() -> errorCallback.onError("Client is shutting down"));
            return;
        }

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(baseUrl + endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    mainHandler.post(() -> responseCallback.onSuccess(sb.toString()));
                } else {
                    mainHandler.post(() -> errorCallback.onError("HTTP " + code));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + endpoint, e);
                mainHandler.post(() -> errorCallback.onError(e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private interface ResponseCallback { void onSuccess(String response); }
    private interface ErrorCallback { void onError(String error); }

    public void shutdown() {
        isShuttingDown = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}