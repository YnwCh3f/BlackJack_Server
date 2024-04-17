module com.example.blackjack_server {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.blackjack_server to javafx.fxml;
    exports com.example.blackjack_server;
}