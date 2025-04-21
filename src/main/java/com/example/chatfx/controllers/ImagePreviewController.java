package com.example.chatfx.controllers;

import com.example.chatfx.ServerConnector;
import com.example.chatfx.enums.OperationCode;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;

public class ImagePreviewController {
    @FXML
    private ImageView image_iv;
    @FXML
    private Button send_btn;
    @FXML
    private Button cancel_btn;

    private Stage stage;

    private byte[] imageData;

    public void setImage(byte[] imageData) {
        this.imageData = imageData;

        // Создание картинки
        Image image = new Image(new ByteArrayInputStream(imageData));
        image_iv.setImage(image);

        // Масштабируем картинку
        double originalWidth = image.getWidth();
        double originalHeight = image.getHeight();
        double newWidth = image_iv.getFitWidth();
        double newHeight = image_iv.getFitHeight();

        if (newWidth / originalWidth * originalHeight <= newHeight) {
            image_iv.setFitWidth(newWidth);
            image_iv.setFitHeight(newWidth / originalWidth * originalHeight);
        } else {
            image_iv.setFitWidth(newHeight / originalHeight * originalWidth);
            image_iv.setFitHeight(newHeight);
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        cancel_btn.setOnAction(e -> stage.close());

        send_btn.setOnAction(e -> {
            ServerConnector serverConnector = ServerConnector.getInstance();
            Gson gson = new Gson();

            HashMap<String, String> map = new HashMap<>();
            map.put("code", OperationCode.IMAGE.stringValue());
            map.put("sender", serverConnector.getUsername());
            map.put("receivers", "");
            map.put("image", Base64.getEncoder().encodeToString(imageData));

            try {
                serverConnector.sendMessage(gson.toJson(map));
            } catch (IOException ex) {
                System.err.println("Ошибка подключения");
            }

            stage.close();
        });
    }
}
