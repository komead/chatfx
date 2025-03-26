package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerHandler;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.ConnectException;

public class MainController {
    @FXML
    private TextArea output_ta;
    @FXML
    private TextField input_tf;
    @FXML
    private Label info;

    private ServerHandler serverHandler = ServerHandler.getInstance();
    private volatile boolean pause = true;

    private final Thread messageReader = new Thread(() -> {
        String receivedMessage = "";

        while (true) {
            // На случай, если нужно где-то приостановить поток
            if (pause) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (!checkConnection())
                continue;

            try {
                receivedMessage = serverHandler.checkMessage();
            } catch (IOException e) {
                info.setText("Ошибка подключения к серверу");
            }

            if (!receivedMessage.isEmpty()) {
                String finalReceivedMessage = receivedMessage;
                output_ta.appendText(finalReceivedMessage + "\n");
                receivedMessage = "";
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    });

    public MainController() {
        // Запускаем поток для прослушивания сообщений от сервера
        messageReader.setDaemon(true);
        messageReader.start();
    }

    @FXML
    public void initialize() {
        try {
            serverHandler.connect();
        } catch (IOException e) {
            info.setText("Не удалось установить соединение с сервером");
        }

        authorize();
    }

    // Метод для извлечения сообщения и его отправки
    @FXML
    private void onSendButtonClick() {
        if (!checkConnection())
            return;

        if (!serverHandler.isAuthorized())
            authorize();

        sendMessage();
    }

    private void sendMessage() {
        if (!input_tf.getText().isEmpty()) {
            serverHandler.sendMessage(input_tf.getText());

            output_ta.appendText(input_tf.getText() + "\n");
            input_tf.setText("");
        }
    }

    /**
     * Метод открывает окно авторизации, приостанавливая поток чтения сообщений
     */
    private void authorize() {
        try {
            // Создаем новое окно
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("authorization-view.fxml"));
            Parent root = loader.load();

            Stage newStage = new Stage();
            newStage.setTitle("Авторизация");

            Scene scene = new Scene(root, 300, 200);
            newStage.setScene(scene);

            pause = true;
            newStage.showAndWait();
            pause = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод возвращает true при наличии соединения
     */
    private boolean checkConnection() {
        try {
            if (serverHandler.isConnected()) {
                info.setText("");
            }
        } catch (NullPointerException | ConnectException e) {
            if (info != null)
                info.setText(e.getMessage());
            return false;
        }

        return true;
    }
}