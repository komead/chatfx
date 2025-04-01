package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerHandler;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
    @FXML
    private ListView<String> users_lv;

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
                        output_ta.appendText(prefix + " send you " + receivedMessage + "\n");
                    }
                } else if (map.get("code").equals("usersList")) {
                    String[] users = map.get("users").split("\\s");
                    users_lv.getItems().addAll(users);
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
        setListeners();

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

        if (input_tf.getText().charAt(0) == '/') {
            String[] arr = input_tf.getText().split("\\s", 2);
            String[] str = arr[0].substring(1, arr.length - 1).split(",");

            if (str[0].isEmpty()) {
                str[0] = arr[0].substring(1, arr[0].length() - 1);
            }

            for (String s : str)
                sendMessage(s, arr[1]);
        } else {
            sendMessage("all", input_tf.getText());
        }

        input_tf.setText("");
    }

    private void setListeners() {
        // Действие при нажатии на элемент списка
        users_lv.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // Извлечение значения элемента, на который нажали
                String selectedItem = users_lv.getSelectionModel().getSelectedItem();

                if (selectedItem != null) {
                    info.setText("Выбран: " + selectedItem);
                    StringBuilder msg = new StringBuilder(input_tf.getText());

                    if (msg.isEmpty() || msg.charAt(0) != '/') {
                        msg.insert(0, "/ ");
                    }

                    String[] arr = msg.toString().split("\\s", 2);
                    if (!arr[0].contains(selectedItem)) {
                        arr[0] += selectedItem + ',';
                    }

                    msg.setLength(0);
                    for (String s : arr) {
                        msg.append(s + ' ');
                    }
                    input_tf.setText(msg.toString());
                }
            }
        });
    }

    /**
     * Метод извлекает текст из текстового поля ввода, чистит поле и отправляет этот текст на сервер.
     * В качестве параметра {@code receiver} передаётся получатель сообщения.
     */
    private void sendMessage(String receiver, String message) {
        if (!message.isEmpty()) {
            HashMap<String, String> data = new HashMap<>();
            data.put("code", "message");
            data.put("text", message);
            data.put("receiver", receiver);
            data.put("sender" , serverHandler.getUsername());

            serverHandler.sendMessage(gson.toJson(data));
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