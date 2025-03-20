package com.example.chatfx;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MainController {
    private ServerHandler serverHandler;

    public MainController() {
        serverHandler = new ServerHandler();
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
}