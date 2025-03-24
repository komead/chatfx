package com.example.chatfx.controllers;

import com.example.chatfx.ServerHandler;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.HashMap;

public class AuthorizationController {
    private ServerHandler serverHandler;
    private Gson gson = new Gson();

    @FXML
    private TextField login_tf;
    @FXML
    private PasswordField password_tf;

    public AuthorizationController() {
        serverHandler = ServerHandler.getInstance();
    }

    @FXML
    private void onLoginButtonClick() {
        if (!serverHandler.isConnected()) {
            // Обработать отсутствие подключения
        }

        if (login_tf.getText().isEmpty() || password_tf.getText().isEmpty()) {
            // Обработать ситуацию с пустыми полями
        }

        HashMap<String, String> data = new HashMap<>();

        data.put("code", "/login");
        data.put("username", login_tf.getText());
        data.put("password", password_tf.getText());

        serverHandler.sendMessage(gson.toJson(data));

        // Нужно обработать ответ
        serverHandler.checkMessage();
    }

    @FXML
    private void onRegisterButtonClick() {
        if (!serverHandler.isConnected()) {
            // Обработать отсутствие подключения
        }

        if (login_tf.getText().isEmpty() || password_tf.getText().isEmpty()) {
            // Обработать ситуацию с пустыми полями
        }

        HashMap<String, String> data = new HashMap<>();

        data.put("code", "/register");
        data.put("username", login_tf.getText());
        data.put("password", password_tf.getText());

        serverHandler.sendMessage(gson.toJson(data));

        // Нужно обработать ответ
        serverHandler.checkMessage();
    }
}
