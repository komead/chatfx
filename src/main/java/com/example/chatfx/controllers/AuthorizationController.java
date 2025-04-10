package com.example.chatfx.controllers;

import com.example.chatfx.ServerConnector;
import com.example.chatfx.enums.OperationCode;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;

public class AuthorizationController {
    private ServerConnector serverConnector;
    private Gson gson = new Gson();

    @FXML
    private TextField login_tf;
    @FXML
    private PasswordField password_tf;
    @FXML
    private Label info;

    public AuthorizationController() {
        serverConnector = ServerConnector.getInstance();
    }

    @FXML
    private void onLoginButtonClick() {
        setInfo("");

        new Thread(() -> {
            if (checkProblems())
                return;

            // Собираем сообщение для отправки
            HashMap<String, String> data = new HashMap<>();
            data.put("code", OperationCode.LOGIN.stringValue());
            data.put("username", login_tf.getText());
            data.put("password", password_tf.getText());

            // Отправляем сообщение и ждём ответ
            try {
                serverConnector.sendMessage(gson.toJson(data));
            } catch (IOException e) {
                setInfo("Нет соединения с сервером");
                serverConnector.setConnected(false);
            }
            setInfo("Ждём ответ от сервера...");
            serverConnector.setUsername(login_tf.getText());
            checkMessage();
        }).start();
    }

    @FXML
    private void onRegisterButtonClick() {
        setInfo("");

        new Thread(() -> {
            if (checkProblems())
                return;

            // Собираем сообщение для отправки
            HashMap<String, String> data = new HashMap<>();
            data.put("code", OperationCode.REGISTRATION.stringValue());
            data.put("username", login_tf.getText());
            data.put("password", password_tf.getText());

            // Отправляем сообщение и ждём ответ
            try {
                serverConnector.sendMessage(gson.toJson(data));
            } catch (IOException e) {
                setInfo("Нет соединения с сервером");
                serverConnector.setConnected(false);
            }
            setInfo("Ждём ответ от сервера...");
            serverConnector.setUsername(login_tf.getText());
            checkMessage();
        }).start();
    }

    /**
     * Метод проверяет перед отправкой сообщения подключение к серверу и корректность введённых данных.
     */
    private boolean checkProblems() {
        if (serverConnector.isConnected()) {
            setInfo("");
        } else {
            setInfo("Нет соединения с сервером");
            try {
                serverConnector.finish();
                serverConnector.connect();
                serverConnector.setConnected(true);
            } catch (IOException e) {
                setInfo("Сервер недоступен");
                return true;
            }
        }

        // При пустых полях ничего происходить не должно
        if (login_tf.getText().isEmpty() || password_tf.getText().isEmpty()) {
            setInfo("Не введён логин или пароль!");
            return true;
        }

        // Ограничение на длину логина и пароля
        if (login_tf.getText().length() > 10 || password_tf.getText().length() > 10) {
            setInfo("Логин и пароль должны быть длиннее 10 символов");
            return true;
        }

        // Запрет на ввод пробелов
        if (login_tf.getText().contains(" ") || password_tf.getText().contains(" ")) {
            setInfo("Логин и пароль не должны содержать пробелов");
            return true;
        }

        return false;
    }

    /**
     * Данный метод ожидает ответ от сервера.
     * При одобрении сервером регистрации или входа закрывается окно авторизации.
     * При отказе сервером выводится информация о проблеме
     */
    private void checkMessage() {
        String answer = "";
        try {
            answer = serverConnector.checkMessage();
        } catch (IOException e) {
            setInfo("Нет соединения с сервером");
            serverConnector.setConnected(false);
        }

        if (answer.isEmpty())
            return;

        HashMap<String, String> data = gson.fromJson(answer, HashMap.class);

        switch (OperationCode.fromValue(data.get("code"))) {
            case ACCESS_GRANTED:
                serverConnector.setAuthorized(true);
                Platform.runLater(() -> {
                    Stage stage = (Stage) info.getScene().getWindow();
                    stage.close();
                });
                break;
            case ACCESS_DENIED:
                setInfo(data.get("body"));
                break;
        }
    }

    private void setInfo(String message) {
        Platform.runLater(() -> info.setText(message));
    }
}
