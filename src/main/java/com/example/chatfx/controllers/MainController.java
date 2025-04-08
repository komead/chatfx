package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerConnector;
import com.example.chatfx.enums.OperationCode;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class MainController {
    @FXML
    private VBox output_vb;
    @FXML
    private TextField input_tf;
    @FXML
    private Label info;
    @FXML
    private ListView<String> users_lv;
    @FXML
    private ScrollPane scrollPane;

    private ServerConnector serverConnector = ServerConnector.getInstance();
    private volatile boolean pause = true;
    private Gson gson = new Gson();

    /**
     * Поток, который слушает сообщения от сервера, обрабатывает их и выводит их на экран
     */
    private final Thread messageReader = new Thread(() -> {
        String receivedMessage;
        HashMap<String, String> messageMap;

        while (true) {
            // На случай, если нужно где-то приостановить поток
            if (pause) {
                pause(1000);
                continue;
            }
            // Если нет соединения с сервером, то пропускаем чтение сообщения
            if (!checkConnection())
                continue;

            // Получаем сообщение и обрабатываем его
            try {
                receivedMessage = serverConnector.checkMessage();
                messageMap = gson.fromJson(receivedMessage, HashMap.class);

                switch (OperationCode.fromValue(messageMap.get("code"))) {
                    case USERS_LIST:
                        usersListAction(messageMap.get("users"));
                        break;
                    case MESSAGE:
                        messageAction(messageMap.get("sender"), messageMap.get("receivers"), messageMap.get("text"));
                        break;
                    case IMAGE:
                        imageAction(messageMap);
                        break;
                    case null:
                    default:
                        System.out.println("Неопознанное действие");
                        break;
                }
            } catch (IOException e) {
                info.setText("Ошибка подключения к серверу");
            }

            pause(1000);
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
        if (input_tf.getText().isEmpty() || !checkConnection())
            return;

        if (!serverConnector.isAuthorized())
            authorize();

        // Если сообщение начинается со слэша, то оно считается личным сообщением.
        if (input_tf.getText().charAt(0) == '/') {
            // После слэша идёт перечисление получателей личного сообщения через запятую, далее через пробел само сообщение
            String[] parts = input_tf.getText().split("\\s", 2); // Отделяем получателей от сообщения
            sendMessage(parts[0].substring(1, parts[0].length() - 1), parts[1]);
        } else {
            sendMessage("", input_tf.getText());
        }

        Platform.runLater(() -> input_tf.setText(""));
    }

    @FXML
    private void onUploadButtonClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        // Устанавливаем фильтры для изображений
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                // Читаем файл в массив байтов
                byte[] imageData = Files.readAllBytes(selectedFile.toPath());
                HashMap<String, String> map = new HashMap<>();
                map.put("code", OperationCode.IMAGE.stringValue());
                map.put("receivers", "");
                map.put("image", Base64.getEncoder().encodeToString(imageData));
                serverConnector.sendMessage(gson.toJson(map));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void onPressedKeyHandler(KeyEvent event) {
        // При нажатии на enter
        if (event.getCode() == KeyCode.ENTER) {
            onSendButtonClick();

            // Задержка от спама
            pause(500);
        }
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

                    Platform.runLater(() -> {
                        input_tf.setText(msg.toString());
                    });
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
            data.put("code", OperationCode.MESSAGE.stringValue());
            data.put("text", message);
            data.put("receivers", receiver);
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

    private void messageAction(String sender, String receivers, String message) {
        String prefix;
        HashSet<String> receiver = new HashSet<>();
        Collections.addAll(receiver, receivers.split(","));

        // Проверяем от кого пришло сообщение и добавляем приписку перед сообщением
        if (sender.equals(serverConnector.getUsername())) {
            prefix = "You";
        } else {
            prefix = sender;
        }

        // Проверяем кому адресовано сообщение
        Text text = new Text();
        if (receiver.contains(serverConnector.getUsername()) && !sender.equals(serverConnector.getUsername())) {
            text.setText(prefix + " send you: " + message);
        } else {
            text.setText(prefix + ": " + message);
        }

        Platform.runLater(() -> {
            output_vb.getChildren().add(text);
            scrollPane.setVvalue(1.0); // Прокрутка вниз
        });
    }

    private void usersListAction(String usersList) {
        // Заполняем список пользователей
        String[] users = usersList.split(",");
        Platform.runLater(() -> {
            users_lv.setItems(FXCollections.observableArrayList(users));
        });
    }

    private void imageAction(HashMap<String, String> map) {
        // Извлечение данных из строки и создание картинки
        byte[] imageData = Base64.getDecoder().decode(map.get("image"));
        Image image = new Image(new ByteArrayInputStream(imageData));
        ImageView imageView = new ImageView(image);

        // Масштабируем картинку до определённой ширины
        double originalWidth = image.getWidth();
        double originalHeight = image.getHeight();
        double newWidth = 200;

        imageView.setFitWidth(newWidth);
        imageView.setFitHeight(newWidth / originalWidth * originalHeight);

        // Добавляем картинку в чат
        Platform.runLater(() -> {
            output_vb.getChildren().add(imageView);
            scrollPane.setVvalue(1.0); // Прокрутка вниз
        });
    }

    private void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}