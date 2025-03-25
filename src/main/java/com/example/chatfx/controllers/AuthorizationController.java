package com.example.chatfx.controllers;

import com.example.chatfx.ServerHandler;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.HashMap;

public class AuthorizationController {
    private ServerHandler serverHandler;
    private Gson gson = new Gson();

    @FXML
    private TextField login_tf;
    @FXML
    private PasswordField password_tf;
    @FXML
    private Label info;

    public AuthorizationController() {
        serverHandler = ServerHandler.getInstance();
    }

    @FXML
    private void onLoginButtonClick() {
        info.setText("");
        if (checkProblems())
            return;

        HashMap<String, String> data = new HashMap<>();

        data.put("code", "/login");
        data.put("username", login_tf.getText());
        data.put("password", password_tf.getText());

        serverHandler.sendMessage(gson.toJson(data));

        // Ждём ответ от сервера
        checkMessage();
    }

    @FXML
    private void onRegisterButtonClick() {
        info.setText("");
        if (checkProblems())
            return;

        HashMap<String, String> data = new HashMap<>();

        data.put("code", "/register");
        data.put("username", login_tf.getText());
        data.put("password", password_tf.getText());

        serverHandler.sendMessage(gson.toJson(data));

        // Ждём ответ от сервера
        checkMessage();
    }

    private boolean checkProblems() {
        if (!serverHandler.isConnected()) {
            info.setText("Нет соединения с сервером");
            // Обработать отсутствие подключения
            return true;
        }

        if (login_tf.getText().isEmpty() || password_tf.getText().isEmpty()) {
            info.setText("Не введён логин или пароль!");
            // Обработать ситуацию с пустыми полями
            return true;
        }

        return false;
    }

    private void checkMessage() {
        String answer = "";
        try {
            answer = serverHandler.checkMessage();
        } catch (IOException e) {
            // Нужно обработать ошибку получения ответа
            e.printStackTrace();
        }

        if (answer.isEmpty())
            return;

        if (answer.startsWith("/ok")) {
            // Обработать одобрение авторизации
        }

        if (answer.startsWith("/deny")) {
            // Обработать отказ в доступе
        }
    }
}
