package com.example.chatfx.controllers;

import com.example.chatfx.ServerHandler;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AuthorizationController {
    private ServerHandler serverHandler;

    @FXML
    private TextField login_tf;
    @FXML
    private PasswordField password_tf;

    public AuthorizationController() {
        serverHandler = ServerHandler.getInstance();
    }

    @FXML
    private void onLoginButtonClick() {
        if (serverHandler.isConnected()) {
            serverHandler.sendMessage("/login");

            // Ждём ответ
            serverHandler.checkMessage();
        }
    }

    @FXML
    private void onRegisterButtonClick() {
        if (serverHandler.isConnected()) {
            serverHandler.sendMessage("/register");

            // Ждём ответ
            serverHandler.checkMessage();
        }
    }
}
