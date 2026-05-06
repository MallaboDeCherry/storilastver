/*
 * Пример кода для NodeMCU32 (ESP32)
 * Управление шторами через WiFi
 */

#include <WiFi.h>
#include <WebServer.h>
#include <ArduinoJson.h>

// WiFi настройки
const char* ssid = "YOUR_WIFI_SSID";
const char* password = "YOUR_WIFI_PASSWORD";

// IP адрес Python сервера
const char* serverIP = "192.168.1.100";
const int serverPort = 5000;

// Пины для управления мотором
const int MOTOR_PIN1 = 12;  // Направление 1
const int MOTOR_PIN2 = 14;  // Направление 2
const int MOTOR_ENABLE = 27; // PWM для скорости

// Состояние штор
int currentPosition = 0;  // 0-100
bool isMoving = false;
String curtainId = "curtain_1";

WebServer server(80);

void setup() {
  Serial.begin(115200);

  // Настройка пинов мотора
  pinMode(MOTOR_PIN1, OUTPUT);
  pinMode(MOTOR_PIN2, OUTPUT);
  pinMode(MOTOR_ENABLE, OUTPUT);

  // Остановка мотора
  stopMotor();

  // Подключение к WiFi
  Serial.println("Подключение к WiFi...");
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("");
  Serial.println("WiFi подключен!");
  Serial.print("IP адрес: ");
  Serial.println(WiFi.localIP());

  // Настройка веб-сервера
  server.on("/command", HTTP_POST, handleCommand);
  server.on("/status", HTTP_GET, handleStatus);

  server.begin();
  Serial.println("HTTP сервер запущен");
}

void loop() {
  server.handleClient();

  // Периодическая отправка статуса на Python сервер
  static unsigned long lastUpdate = 0;
  if (millis() - lastUpdate > 5000) {  // Каждые 5 секунд
    sendStatusToServer();
    lastUpdate = millis();
  }
}

// Обработка команд от сервера
void handleCommand() {
  if (server.hasArg("plain")) {
    String body = server.arg("plain");
    Serial.println("Получена команда: " + body);

    StaticJsonDocument<200> doc;
    DeserializationError error = deserializeJson(doc, body);

    if (error) {
      server.send(400, "application/json", "{\"success\":false,\"error\":\"Invalid JSON\"}");
      return;
    }

    String action = doc["action"];
    int targetPosition = doc["position"] | -1;

    if (action == "open") {
      moveToPosition(100);
    } else if (action == "close") {
      moveToPosition(0);
    } else if (action == "stop") {
      stopMotor();
    } else if (action == "set_position" && targetPosition >= 0) {
      moveToPosition(targetPosition);
    }

    // Отправка ответа
    StaticJsonDocument<200> response;
    response["success"] = true;
    response["position"] = currentPosition;
    response["is_moving"] = isMoving;

    String responseStr;
    serializeJson(response, responseStr);
    server.send(200, "application/json", responseStr);
  } else {
    server.send(400, "application/json", "{\"success\":false,\"error\":\"No data\"}");
  }
}

// Получение текущего статуса
void handleStatus() {
  StaticJsonDocument<200> doc;
  doc["curtain_id"] = curtainId;
  doc["position"] = currentPosition;
  doc["is_moving"] = isMoving;
  doc["status"] = getStatus();

  String response;
  serializeJson(doc, response);
  server.send(200, "application/json", response);
}

// Движение к заданной позиции
void moveToPosition(int targetPosition) {
  if (targetPosition < 0) targetPosition = 0;
  if (targetPosition > 100) targetPosition = 100;

  Serial.print("Движение к позиции: ");
  Serial.println(targetPosition);

  isMoving = true;

  if (targetPosition > currentPosition) {
    // Открытие (движение вперед)
    digitalWrite(MOTOR_PIN1, HIGH);
    digitalWrite(MOTOR_PIN2, LOW);
    analogWrite(MOTOR_ENABLE, 255);  // Полная скорость
  } else if (targetPosition < currentPosition) {
    // Закрытие (движение назад)
    digitalWrite(MOTOR_PIN1, LOW);
    digitalWrite(MOTOR_PIN2, HIGH);
    analogWrite(MOTOR_ENABLE, 255);
  } else {
    stopMotor();
    return;
  }

  // Симуляция движения (в реальности нужны датчики положения)
  // Примерный расчет: 1% = 100мс движения
  int diff = abs(targetPosition - currentPosition);
  delay(diff * 100);

  currentPosition = targetPosition;
  stopMotor();
}

// Остановка мотора
void stopMotor() {
  digitalWrite(MOTOR_PIN1, LOW);
  digitalWrite(MOTOR_PIN2, LOW);
  analogWrite(MOTOR_ENABLE, 0);
  isMoving = false;
  Serial.println("Мотор остановлен");
}

// Получение статуса
String getStatus() {
  if (isMoving) return "moving";
  if (currentPosition == 0) return "closed";
  if (currentPosition == 100) return "open";
  return "stopped";
}

// Отправка статуса на Python сервер
void sendStatusToServer() {
  if (WiFi.status() != WL_CONNECTED) return;

  WiFiClient client;

  if (client.connect(serverIP, serverPort)) {
    StaticJsonDocument<200> doc;
    doc["curtain_id"] = curtainId;
    doc["position"] = currentPosition;
    doc["is_moving"] = isMoving;
    doc["status"] = getStatus();

    String jsonStr;
    serializeJson(doc, jsonStr);

    client.println("POST /api/nodemcu/status HTTP/1.1");
    client.print("Host: ");
    client.println(serverIP);
    client.println("Content-Type: application/json");
    client.print("Content-Length: ");
    client.println(jsonStr.length());
    client.println();
    client.println(jsonStr);

    client.stop();
    Serial.println("Статус отправлен на сервер");
  }
}
