#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Сервер для управления умными шторами через NodeMCU32
Обрабатывает JSON запросы от Android приложения и передает команды на NodeMCU32
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import time
import threading
import requests
from datetime import datetime

app = Flask(__name__)
CORS(app)  # Разрешить CORS для Android приложения

# Конфигурация
NODEMCU_IP = "192.168.1.101"  # IP адрес вашего NodeMCU32
NODEMCU_PORT = 80

# Хранилище состояний штор (в реальном приложении можно использовать базу данных)
curtains_state = {
    "curtain_1": {
        "id": "curtain_1",
        "name": "Штора 1",
        "position": 0,
        "is_moving": False,
        "status": "closed",
        "last_update": int(time.time() * 1000)
    }
}

# Блокировка для потокобезопасности
state_lock = threading.Lock()


def send_to_nodemcu(command_data):
    """
    Отправка команды на NodeMCU32
    """
    try:
        url = f"http://{NODEMCU_IP}:{NODEMCU_PORT}/command"
        response = requests.post(url, json=command_data, timeout=5)

        if response.status_code == 200:
            return True, response.json()
        else:
            return False, {"error": f"NodeMCU вернул код {response.status_code}"}
    except requests.exceptions.Timeout:
        return False, {"error": "Таймаут соединения с NodeMCU"}
    except requests.exceptions.ConnectionError:
        return False, {"error": "Не удалось подключиться к NodeMCU"}
    except Exception as e:
        return False, {"error": str(e)}


def update_curtain_state(curtain_id, **kwargs):
    """
    Обновление состояния штор
    """
    with state_lock:
        if curtain_id in curtains_state:
            curtains_state[curtain_id].update(kwargs)
            curtains_state[curtain_id]["last_update"] = int(time.time() * 1000)


@app.route('/api/ping', methods=['GET'])
def ping():
    """
    Проверка доступности сервера
    """
    return jsonify({
        "success": True,
        "message": "Сервер работает",
        "timestamp": int(time.time() * 1000)
    })


@app.route('/api/curtains', methods=['GET'])
def get_all_curtains():
    """
    Получение списка всех штор
    """
    with state_lock:
        curtains_list = list(curtains_state.values())

    return jsonify({
        "success": True,
        "curtains": curtains_list
    })


@app.route('/api/curtain/state/<curtain_id>', methods=['GET'])
def get_curtain_state(curtain_id):
    """
    Получение состояния конкретной шторы
    """
    with state_lock:
        if curtain_id in curtains_state:
            return jsonify({
                "success": True,
                "curtain_state": curtains_state[curtain_id]
            })
        else:
            return jsonify({
                "success": False,
                "error": "Штора не найдена"
            }), 404


@app.route('/api/curtain/command', methods=['POST'])
def send_command():
    """
    Отправка команды на управление шторами
    """
    try:
        data = request.get_json()

        if not data:
            return jsonify({
                "success": False,
                "error": "Нет данных в запросе"
            }), 400

        curtain_id = data.get('curtain_id')
        action = data.get('action')
        position = data.get('position', -1)

        if not curtain_id or not action:
            return jsonify({
                "success": False,
                "error": "Отсутствуют обязательные поля"
            }), 400

        # Проверка существования шторы
        if curtain_id not in curtains_state:
            return jsonify({
                "success": False,
                "error": "Штора не найдена"
            }), 404

        # Подготовка команды для NodeMCU
        nodemcu_command = {
            "curtain_id": curtain_id,
            "action": action,
            "timestamp": int(time.time() * 1000)
        }

        # Обработка различных команд
        if action == "open":
            update_curtain_state(curtain_id, is_moving=True, status="moving")
            nodemcu_command["position"] = 100

        elif action == "close":
            update_curtain_state(curtain_id, is_moving=True, status="moving")
            nodemcu_command["position"] = 0

        elif action == "stop":
            update_curtain_state(curtain_id, is_moving=False, status="stopped")

        elif action == "set_position":
            if position < 0 or position > 100:
                return jsonify({
                    "success": False,
                    "error": "Позиция должна быть от 0 до 100"
                }), 400

            update_curtain_state(curtain_id, is_moving=True, status="moving", position=position)
            nodemcu_command["position"] = position

        else:
            return jsonify({
                "success": False,
                "error": "Неизвестная команда"
            }), 400

        # Отправка команды на NodeMCU
        success, response = send_to_nodemcu(nodemcu_command)

        if success:
            # Симуляция завершения движения через некоторое время
            if action in ["open", "close", "set_position"]:
                def finish_movement():
                    time.sleep(3)  # Симуляция времени движения
                    new_position = 100 if action == "open" else (0 if action == "close" else position)
                    new_status = "open" if new_position == 100 else ("closed" if new_position == 0 else "stopped")
                    update_curtain_state(curtain_id,
                                       position=new_position,
                                       is_moving=False,
                                       status=new_status)

                threading.Thread(target=finish_movement, daemon=True).start()

            with state_lock:
                current_state = curtains_state[curtain_id].copy()

            return jsonify({
                "success": True,
                "message": "Команда выполнена",
                "curtain_state": current_state
            })
        else:
            return jsonify({
                "success": False,
                "error": response.get("error", "Ошибка отправки команды"),
                "message": "Не удалось отправить команду на NodeMCU"
            }), 500

    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/api/nodemcu/status', methods=['POST'])
def receive_nodemcu_status():
    """
    Получение обновлений статуса от NodeMCU32
    NodeMCU может отправлять сюда обновления о текущем состоянии
    """
    try:
        data = request.get_json()

        curtain_id = data.get('curtain_id')
        position = data.get('position')
        is_moving = data.get('is_moving', False)
        status = data.get('status', 'unknown')

        if curtain_id and curtain_id in curtains_state:
            update_curtain_state(curtain_id,
                               position=position,
                               is_moving=is_moving,
                               status=status)

            return jsonify({
                "success": True,
                "message": "Статус обновлен"
            })
        else:
            return jsonify({
                "success": False,
                "error": "Неверный ID шторы"
            }), 400

    except Exception as e:
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


@app.route('/api/config', methods=['GET', 'POST'])
def config():
    """
    Управление конфигурацией сервера
    """
    global NODEMCU_IP, NODEMCU_PORT

    if request.method == 'GET':
        return jsonify({
            "success": True,
            "config": {
                "nodemcu_ip": NODEMCU_IP,
                "nodemcu_port": NODEMCU_PORT
            }
        })

    elif request.method == 'POST':
        data = request.get_json()

        if 'nodemcu_ip' in data:
            NODEMCU_IP = data['nodemcu_ip']

        if 'nodemcu_port' in data:
            NODEMCU_PORT = data['nodemcu_port']

        return jsonify({
            "success": True,
            "message": "Конфигурация обновлена",
            "config": {
                "nodemcu_ip": NODEMCU_IP,
                "nodemcu_port": NODEMCU_PORT
            }
        })


if __name__ == '__main__':
    print("=" * 60)
    print("Сервер управления умными шторами")
    print("=" * 60)
    print(f"Запуск сервера на http://0.0.0.0:5000")
    print(f"NodeMCU32 адрес: http://{NODEMCU_IP}:{NODEMCU_PORT}")
    print("=" * 60)
    print("\nДоступные эндпоинты:")
    print("  GET  /api/ping                    - Проверка сервера")
    print("  GET  /api/curtains                - Список всех штор")
    print("  GET  /api/curtain/state/<id>      - Состояние шторы")
    print("  POST /api/curtain/command         - Отправка команды")
    print("  POST /api/nodemcu/status          - Обновление от NodeMCU")
    print("  GET  /api/config                  - Получить конфигурацию")
    print("  POST /api/config                  - Обновить конфигурацию")
    print("=" * 60)

    app.run(host='0.0.0.0', port=5000, debug=True)
