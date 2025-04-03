package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerConnector;
import com.google.gson.Gson;
import javafx.collections.FXCollections;
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

    private ServerConnector serverConnector = ServerConnector.getInstance();
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
                receivedMessage = serverConnector.checkMessage();
                map = gson.fromJson(receivedMessage, HashMap.class);

                // На данный момент может прийти только два типа содержимого: простое сообщение и список пользователей
                if (map.get("code").equals("message")) {
                    messageAction(map.get("sender"), map.get("receiver"), receivedMessage);
                } else if (map.get("code").equals("usersList")) {
                    usersListAction(map.get("users"));
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
            serverConnector.connect();
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

        if (!serverConnector.isAuthorized())
            authorize();

        // Если сообщение начинается со слэша, то оно считается личным сообщением.
        if (input_tf.getText().charAt(0) == '/') {
            // После слэша идёт перечисление получателей личного сообщения через запятую, далее через пробел само сообщение
            String[] parts = input_tf.getText().split("\\s", 2); // Отделяем получателей от сообщения
            String[] receivers = parts[0].substring(1, parts.length - 1).split(","); // Разделяем получателей между собой

            // Если получатель один, то массив будет пустой и нужно добавить этого получателя
            if (receivers[0].isEmpty()) {
                receivers[0] = parts[0].substring(1, parts[0].length() - 1);
            }

            // Отправляем сообщение каждому получателю
            for (String s : receivers)
                sendMessage(s, parts[1]);
        } else {
            sendMessage("all", input_tf.getText());
        }

        input_tf.setText("");
    }

    /**
     * Установка слушателей для элементов интерфейса.
     */
    private void setListeners() {
        // Действие при нажатии на элемент списка
        users_lv.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // Извлечение значения элемента, на который нажали
                String selectedItem = users_lv.getSelectionModel().getSelectedItem();

                if (selectedItem != null) {
                    StringBuilder msg = new StringBuilder(input_tf.getText());

                    // Личное сообщение должно начинаться со слэша
                    if (msg.isEmpty() || msg.charAt(0) != '/') {
                        msg.insert(0, "/ ");
                    }

                    // После слэша идёт перечисление получателей личного сообщения через запятую, далее через пробел само сообщение.
                    // Берём первую часть. Если получатель ещё не был указан, то указываем его.
                    String[] arr = msg.toString().split("\\s", 2);
                    if (!arr[0].contains(selectedItem)) {
                        arr[0] += selectedItem + ',';
                    }

                    // Склеиваем всё обратно и возвращаем в поле ввода
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
            data.put("sender" , serverConnector.getUsername());

            serverConnector.sendMessage(gson.toJson(data));
        }
    }

    /**
     * Метод открывает окно входа, приостанавливая поток чтения сообщений
     */
    private void authorize() {
        try {
            // Создаем новое окно
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("authorization-view.fxml"));
            Parent root = loader.load();

            Stage newStage = new Stage();
            newStage.setResizable(false);
            newStage.setTitle("Авторизация");

            Scene scene = new Scene(root);
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
            if (serverConnector.isConnected()) {
                info.setText("");
            }
        } catch (NullPointerException | ConnectException e) {
            if (info != null)
                info.setText("Нет связи с сервером");
            return false;
        }

        return true;
    }

    private void messageAction(String sender, String receiver, String message) {
        String prefix;

        // Проверяем от кого пришло сообщение и добавляем приписку перед сообщением
        if (sender.equals(serverConnector.getUsername())) {
            prefix = "You: ";
        } else {
            prefix = sender;
        }

        // Проверяем кому адресовано сообщение
        if (receiver.equals("all")) {
            output_ta.appendText(prefix + message + "\n");
        } else {
            output_ta.appendText(prefix + " send you: " + message + "\n");
        }
    }

    private void usersListAction(String usersList) {
        // Заполняем список пользователей
        String[] users = usersList.split(",");
        users_lv.setItems(FXCollections.observableArrayList(users));
//        users_lv.getItems().addAll(users);
    }
}