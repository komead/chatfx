module com.example.chatfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;


    opens com.example.chatfx to javafx.fxml;
    exports com.example.chatfx;
    exports com.example.chatfx.controllers;
    opens com.example.chatfx.controllers to javafx.fxml;
}