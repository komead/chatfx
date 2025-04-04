package com.example.chatfx.controllers;

import com.example.chatfx.ServerConnector;
import com.google.gson.Gson;
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
        info.setText("");
        if (checkProblems())
            return;

        // Собираем сообщение для отправки
        HashMap<String, String> data = new HashMap<>();
        data.put("code", "login");
        data.put("username", login_tf.getText());
        data.put("password", password_tf.getText());

        // Отправляем сообщение и ждём ответ
        serverConnector.sendMessage(gson.toJson(data));
        info.setText("Ждём ответ от сервера...");
        serverConnector.setUsername(login_tf.getText());
        checkMessage();
    }

    @FXML
    private void onRegisterButtonClick() {
        info.setText("");
        if (checkProblems())
            return;

        // Собираем сообщение для отправки
        HashMap<String, String> data = new HashMap<>();
        data.put("code", "register");
        data.put("username", login_tf.getText());
        data.put("password", password_tf.getText());

        // Отправляем сообщение и ждём ответ
        serverConnector.sendMessage(gson.toJson(data));
        info.setText("Ждём ответ от сервера...");
        serverConnector.setUsername(login_tf.getText());
        checkMessage();
    }

    /**
     * Метод проверяет перед отправкой сообщения подключение к серверу и корректность введённых данных.
     */
    private boolean checkProblems() {
        try {
            if (serverConnector.isConnected()) {
                info.setText("");
            }
        } catch (NullPointerException e) {
            info.setText(e.getMessage());
            return true;
        } catch (ConnectException e) {
            info.setText(e.getMessage());
            return true;
        }

        // При пустых полях ничего происходить не должно
        if (login_tf.getText().isEmpty() || password_tf.getText().isEmpty()) {
            info.setText("Не введён логин или пароль!");
            return true;
        }

        // Ограничение на длину логина и пароля
        if (login_tf.getText().length() > 10 || password_tf.getText().length() > 10) {
            info.setText("Логин и пароль должны быть длиннее 10 символов");
            return true;
        }

        // Запрет на ввод пробелов
        if (login_tf.getText().contains(" ") || password_tf.getText().contains(" ")) {
            info.setText("Логин и пароль не должны содержать пробелов");
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
            info.setText("Потеряно соединение с сервером");
        }

        if (answer.isEmpty())
            return;

        HashMap<String, String> data = gson.fromJson(answer, HashMap.class);

        switch (data.get("code")) {
            case "ok":
                serverConnector.setAuthorized(true);
                Stage stage = (Stage) info.getScene().getWindow();
                stage.close();
                break;
            case "deny":
                info.setText(data.get("body"));
                break;
        }
    }
}
