package com.example.sshtori.network;

import android.os.Handler;
import android.os.Looper;

import com.example.sshtori.models.CurtainCommand;
import com.example.sshtori.models.CurtainState;
import com.example.sshtori.models.ServerResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Клиент для взаимодействия с Python сервером
 */
public class CurtainApiClient {
    private String serverUrl;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public CurtainApiClient(String serverUrl) {
        this.serverUrl = serverUrl;
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Отправка команды на сервер
     */
    public void sendCommand(CurtainCommand command, ApiCallback<ServerResponse> callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(serverUrl + "/api/curtain/command");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                // Отправка JSON
                OutputStream os = connection.getOutputStream();
                os.write(command.toJSON().toString().getBytes("UTF-8"));
                os.close();

                // Чтение ответа
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    ServerResponse serverResponse = ServerResponse.fromJSON(jsonResponse);

                    mainHandler.post(() -> callback.onSuccess(serverResponse));
                } else {
                    mainHandler.post(() -> callback.onError("Ошибка сервера: " + responseCode));
                }

                connection.disconnect();
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> callback.onError("Ошибка соединения: " + e.getMessage()));
            }
        });
    }

    /**
     * Получение состояния штор
     */
    public void getCurtainState(String curtainId, ApiCallback<CurtainState> callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(serverUrl + "/api/curtain/state/" + curtainId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    if (jsonResponse.has("curtain_state")) {
                        JSONObject stateJson = jsonResponse.getJSONObject("curtain_state");
                        CurtainState state = new CurtainState();
                        state.setId(stateJson.optString("id", ""));
                        state.setName(stateJson.optString("name", ""));
                        state.setPosition(stateJson.optInt("position", 0));
                        state.setMoving(stateJson.optBoolean("is_moving", false));
                        state.setStatus(stateJson.optString("status", "unknown"));
                        state.setLastUpdate(stateJson.optLong("last_update", System.currentTimeMillis()));

                        mainHandler.post(() -> callback.onSuccess(state));
                    } else {
                        mainHandler.post(() -> callback.onError("Неверный формат ответа"));
                    }
                } else {
                    mainHandler.post(() -> callback.onError("Ошибка сервера: " + responseCode));
                }

                connection.disconnect();
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> callback.onError("Ошибка соединения: " + e.getMessage()));
            }
        });
    }

    /**
     * Получение списка всех штор
     */
    public void getAllCurtains(ApiCallback<List<CurtainState>> callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(serverUrl + "/api/curtains");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray curtainsArray = jsonResponse.getJSONArray("curtains");

                    List<CurtainState> curtains = new ArrayList<>();
                    for (int i = 0; i < curtainsArray.length(); i++) {
                        JSONObject curtainJson = curtainsArray.getJSONObject(i);
                        CurtainState state = new CurtainState();
                        state.setId(curtainJson.optString("id", ""));
                        state.setName(curtainJson.optString("name", ""));
                        state.setPosition(curtainJson.optInt("position", 0));
                        state.setMoving(curtainJson.optBoolean("is_moving", false));
                        state.setStatus(curtainJson.optString("status", "unknown"));
                        state.setLastUpdate(curtainJson.optLong("last_update", System.currentTimeMillis()));
                        curtains.add(state);
                    }

                    mainHandler.post(() -> callback.onSuccess(curtains));
                } else {
                    mainHandler.post(() -> callback.onError("Ошибка сервера: " + responseCode));
                }

                connection.disconnect();
            } catch (IOException | JSONException e) {
                mainHandler.post(() -> callback.onError("Ошибка соединения: " + e.getMessage()));
            }
        });
    }

    /**
     * Проверка соединения с сервером
     */
    public void checkConnection(ApiCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                URL url = new URL(serverUrl + "/api/ping");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(3000);

                int responseCode = connection.getResponseCode();
                connection.disconnect();

                boolean isConnected = (responseCode == HttpURLConnection.HTTP_OK);
                mainHandler.post(() -> callback.onSuccess(isConnected));
            } catch (IOException e) {
                mainHandler.post(() -> callback.onSuccess(false));
            }
        });
    }

    /**
     * Интерфейс для обратных вызовов API
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String error);
    }

    /**
     * Освобождение ресурсов
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
