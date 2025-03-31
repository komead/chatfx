package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerHandler;
import com.google.gson.Gson;
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
import java.util.HashMap;

public class MainController {
    @FXML
    private TextArea output_ta;
    @FXML
    private TextField input_tf;
    @FXML
    private Label info;

    private ServerHandler serverHandler = ServerHandler.getInstance();
    private volatile boolean pause = true;
    private Gson gson = new Gson();

    /**
     * Поток, который слушает сообщения от сервера, обрабатывает их и выводит их на экран
     */
    private final Thread messageReader = new Thread(() -> {
        String receivedMessage;
        HashMap<String, String> map;

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
            // Если нет соединения с сервером, то пропускаем чтение сообщения
            if (!checkConnection())
                continue;

            try {
                receivedMessage = serverHandler.checkMessage();
                map = gson.fromJson(receivedMessage, HashMap.class);

                if (map.get("code").equals("message")) {
                    String prefix;

                    if (map.get("sender").equals(serverHandler.getUsername())) {
                        prefix = "You: ";
                    } else {
                        prefix = map.get("sender") + ": ";
                    }

                    if (map.get("receiver").equals("all")) {
                        output_ta.appendText(prefix + receivedMessage + "\n");
                    } else {

                    }
                }
            } catch (IOException e) {
                info.setText("Ошибка подключения к серверу");
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

    /**
     * Метод реагирующий на нажатие кнопки "Отправить"
     */
    @FXML
    private void onSendButtonClick() {
        if (!checkConnection())
            return;

        if (!serverHandler.isAuthorized())
            authorize();

        sendMessage("all");
    }

    /**
     * Метод извлекает текст из текстового поля ввода, чистит поле и отправляет этот текст на сервер.
     * В качестве параметра {@code receiver} передаётся получатель сообщения.
     */
    private void sendMessage(String receiver) {
        if (!input_tf.getText().isEmpty()) {
            HashMap<String, String> data = new HashMap<>();
            data.put("code", "message");
            data.put("text", input_tf.getText());
            data.put("receiver", receiver);

            serverHandler.sendMessage(gson.toJson(data));

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