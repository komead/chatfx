package com.example.chatfx.controllers;

import com.example.chatfx.HelloApplication;
import com.example.chatfx.ServerConnector;
import com.example.chatfx.enums.OperationCode;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.util.*;

public class MainController {
    @FXML
    private VBox output_vb;
    @FXML
    private VBox users_vb;
    @FXML
    private TextField input_tf;
    @FXML
    private Label info;
    @FXML
    private ScrollPane scrollPane;

    private ObservableList<Node> itemsList;

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
            if (!serverConnector.isConnected())
                continue;

            // Получаем сообщение и обрабатываем его
            try {
                receivedMessage = serverConnector.checkMessage();
                messageMap = gson.fromJson(receivedMessage, HashMap.class);
                setInfo("");

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
                setInfo("Ошибка подключения к серверу");
                serverConnector.setConnected(false);
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
        itemsList = users_vb.getChildren();

        if (!serverConnector.isConnected()) {
            try {
                serverConnector.connect();
            } catch (IOException e) {
                setInfo("Не удалось установить соединение с сервером");
            }
        }

        authorize();

        if (serverConnector.isAuthorized())
            setInfo("");
    }

    /**
     * Метод реагирующий на нажатие кнопки "Отправить"
     */
    @FXML
    private void onSendButtonClick() {
        if (input_tf.getText().isEmpty())
            return;

        if (!serverConnector.isConnected()) {
            reconnect();
            return;
        }

        if (!serverConnector.isAuthorized())
            authorize();

        HashSet<String> receivers = getSelectedUsers();

        if (receivers.isEmpty()) {
            sendMessage("", input_tf.getText());
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            for (String receiver : receivers) {
                stringBuilder.append(receiver + ',');
            }

            sendMessage(stringBuilder.toString(), input_tf.getText());
        }

        Platform.runLater(() -> input_tf.setText(""));
    }

    @FXML
    private void onUploadButtonClick() {
        // Открываем окно выбора файлов
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выберите изображение");
        // Устанавливаем фильтры для изображений
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                // Читаем файл в массив байтов
                byte[] imageData = Files.readAllBytes(selectedFile.toPath());

                // Открываем окно просмотра картинки
                FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("imagePreview-view.fxml"));
                Parent root = loader.load();

                ImagePreviewController controller = loader.getController();
                controller.setImage(imageData);

                Stage stage = new Stage();
                stage.setResizable(false);
                controller.setStage(stage);

                stage.setScene(new Scene(root));
                stage.setTitle("Просмотр изображения");
                stage.initModality(Modality.WINDOW_MODAL); // Блокируем родительское окно
                stage.initOwner(output_vb.getScene().getWindow()); // Устанавливаем родительское окно
                stage.show();

                setInfo("");
            } catch (IOException e) {
                setInfo("Ошибка подключения к серверу");
                serverConnector.setConnected(false);
            }
        }
    }

    @FXML
    private void onUncheckButtonClick() {
        for (Node item : itemsList) {
            // Если элемент является CheckBox
            if (item instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) item;

                if (checkBox.isSelected()) {
                    checkBox.setSelected(false);
                }
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
     * Метод возвращает список выбранных пользователей
     */
    public HashSet<String> getSelectedUsers() {
        HashSet<String> selected = new HashSet<>();

        for (Node item : itemsList) {
            // Если элемент является CheckBox
            if (item instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) item;

                if (checkBox.isSelected()) {
                    selected.add(checkBox.getText());
                }
            }
        }

        return selected;
    }

    /**
     * Метод для обновления списка пользователей новыми данными
     */
    public void updateUsersList(Set<String> newItems) {
        List<Node> toRemove = new ArrayList<>();

        for (Node item : itemsList) {
            // Если элемент является CheckBox
            if (item instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) item;

                if (!newItems.contains(checkBox.getText())) {
                    // Удаляем из текущего списка, то, чего нет в новом списке
                    toRemove.add(item);
                } else {
                    // Удаляем в новом списке то, что уже есть в текущем
                    newItems.remove(checkBox.getText());
                }
            }
        }

        itemsList.removeAll(toRemove);

        for (String item : newItems) {
            itemsList.add(new CheckBox(item));
        }
    }

    /**
     * Метод извлекает текст из текстового поля ввода, чистит поле и отправляет этот текст на сервер.
     * В качестве параметра {@code receiver} передаётся получатель сообщения.
     */
    private void sendMessage(String receiver, String message) {
        if (!message.isEmpty()) {
            new Thread(() -> {
                HashMap<String, String> data = new HashMap<>();
                data.put("code", OperationCode.MESSAGE.stringValue());
                data.put("text", message);
                data.put("receivers", receiver);
                data.put("sender" , serverConnector.getUsername());

                try {
                    serverConnector.sendMessage(gson.toJson(data));
                    setInfo("");
                } catch (IOException e) {
                    serverConnector.setConnected(false);
                    setInfo("Нет соединения с сервером");
                    reconnect();
                }
            }).start();
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

    private void messageAction(String sender, String receivers, String message) {
        new Thread(() -> {
            HashSet<String> receiver = new HashSet<>();
            Collections.addAll(receiver, receivers.split(","));

            Text text = new Text();
            text.setText(getPrefix(sender, receiver) + message);

            Platform.runLater(() -> {
                output_vb.getChildren().add(text);
            });

            pause(100);
            // Прокрутка вниз
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }).start();
    }

    private String getPrefix(String sender, HashSet<String> receivers) {
        StringBuilder prefix = new StringBuilder();

        // Проверяем от кого пришло сообщение и добавляем приписку перед сообщением
        if (sender.equals(serverConnector.getUsername())) {
            prefix.append("You");
        } else {
            prefix.append(sender);
        }

        // Проверяем кому адресовано сообщение
        if (receivers != null && receivers.contains(serverConnector.getUsername()) && !sender.equals(serverConnector.getUsername())) {
            prefix.append(" send you: ");
        } else {
            prefix.append(": ");
        }
        return prefix.toString();
    }

    private void usersListAction(String usersList) {
        HashSet<String> users = new HashSet<>();
        for (String user : usersList.split(",")) {
            users.add(user);
        }

        Platform.runLater(() -> {
            updateUsersList(users);
        });
    }

    private void imageAction(HashMap<String, String> map) {
        new Thread(() -> {
            // Извлечение данных из строки и создание картинки
            byte[] imageData = Base64.getDecoder().decode(map.get("image"));
            Image image = new Image(new ByteArrayInputStream(imageData));
            ImageView imageView = new ImageView(image);

            // Масштабируем картинку до определённой ширины
            double originalWidth = image.getWidth();
            double originalHeight = image.getHeight();
            double newWidth = 500;

            imageView.setFitWidth(newWidth);
            imageView.setFitHeight(newWidth / originalWidth * originalHeight);

            // Добавляем картинку в чат
            Platform.runLater(() -> {
                output_vb.getChildren().add(new Text(getPrefix(map.get("sender"), null)));
                output_vb.getChildren().add(imageView);
            });

            pause(100);
            // Прокрутка вниз
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        }).start();
    }

    private void reconnect() {
        new Thread(() -> {
            try {
                serverConnector.reconnect();
                serverConnector.setConnected(true);

                setInfo("");
            } catch (IOException e) {
                serverConnector.setConnected(true);
                setInfo("Сервер недоступен");
            }
        }).start();
    }

    private void pause(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setInfo(String message) {
        Platform.runLater(() -> info.setText(message));
    }
}