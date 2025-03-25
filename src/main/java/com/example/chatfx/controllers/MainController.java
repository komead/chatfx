package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class MainController {
    @FXML
    private TextArea output_ta;
    @FXML
    private TextField input_tf;
    @FXML
    private Label info;

    private ServerHandler serverHandler;

    private Thread messageReader = new Thread(() -> {
        String receivedMessage = "";

        while (serverHandler.isConnected()) {

            try {
                receivedMessage = serverHandler.checkMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }

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

    @FXML
    public void initialize() {
        serverHandler = ServerHandler.getInstance();
        try {
            serverHandler.connect();

            // Начинаем прослушивать сообщения от сервера
            messageReader.setDaemon(true);
            messageReader.start();
        } catch (IOException e) {
            info.setText("Не удалось установить соединение с сервером");
        }
    }

    // Метод для извлечения сообщения и его отправки
    @FXML
    private void onSendButtonClick() {
        if (!serverHandler.isConnected()) {

        }
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

    private void authorize() {
        try {
            // Создаем новое окно
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("authorization-view.fxml"));
            Parent root = loader.load();

            Stage newStage = new Stage();
            newStage.setTitle("Авторизация");

            Scene scene = new Scene(root, 300, 200);
            newStage.setScene(scene);

//            messageReader.wait();//????????????????????????????????????
            newStage.showAndWait();
//            messageReader.run();//????????????????????????????????????
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}