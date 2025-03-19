module com.example.chatfx {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.chatfx to javafx.fxml;
    exports com.example.chatfx;
}