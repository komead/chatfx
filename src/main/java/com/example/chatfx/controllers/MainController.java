package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class MainController {
    private ServerHandler serverHandler;

    public MainController() {
        serverHandler = new ServerHandler();
//        authorize();
//        serverHandler.connect();
//        startListening();
    }

    @FXML
    private TextArea output_ta;
    @FXML
    private TextField input_tf;

    // Метод для извлечения сообщения и его отправки
    @FXML
    private void onSendButtonClick() {
        sendMessage();
    }

    private void sendMessage() {
        if (!input_tf.getText().isEmpty()) {
//            serverHandler.sendMessage(input_tf.getText());

            output_ta.appendText(input_tf.getText() + "\n");
            input_tf.setText("");
        }
    }

    // тут будем прослушивать сообщения от сервера
    private void startListening() {
        Thread t = new Thread(() -> {
            String receivedMessage;

            while (serverHandler.isConnected()) {
                receivedMessage = serverHandler.checkMessage();

                if (!receivedMessage.isEmpty()) {
                    output_ta.appendText(receivedMessage + "\n");
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void authorize() {
        try {
            // Создаем новое окно (Stage)
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("authorization-view.fxml"));
            Parent root = loader.load();

            Stage newStage = new Stage();
            newStage.setTitle("Авторизация");

            Scene scene = new Scene(root, 300, 200);
            newStage.setScene(scene);

            newStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}