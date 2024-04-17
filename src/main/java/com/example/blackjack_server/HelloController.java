package com.example.blackjack_server;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class HelloController {

    @FXML private Button btStart;
    @FXML private ListView<String> lvList;

    public void initialize(){
        Server s = new Server();
        if (lvList.getItems().size() > 5) btStart.setStyle("-fx-text-fill: green;");
    }

    @FXML private void onStartClick(){
        if (lvList.getItems().size() > 5){

        }
    }

}