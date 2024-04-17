package com.example.blackjack_server;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class HelloController {

    @FXML private Button btStart;
    @FXML private ListView<String> lvList;
    private Server s = new Server();

    public void initialize(){
        if (s.clients.size() >= 7) btStart.setStyle("-fx-text-fill: green;");
    }

    @FXML private void onStartClick(){
        for (Client x : s.clients){
            s.send("start:" + lvList.getItems().size(), s.clients.indexOf(x));
        }

    }

}