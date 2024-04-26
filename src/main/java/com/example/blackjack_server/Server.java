package com.example.blackjack_server;

import com.example.blackjack_server.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Server {

    public ArrayList<String> cards = new ArrayList<>();
    public ArrayList<Client> clients = new ArrayList<>();
    public Stack<String> deck = new Stack<>();
    public HashSet<String> royals = new HashSet<>();
    public int sumCards = 0;
    public boolean full = false;
    public boolean joinable = true;

    public Server() {
        royals.add("J");
        royals.add("Q");
        royals.add("K");
        for(int x = 0; x < 6; x++) {
            for (int i = 2; i < 11; i++) {
                for (int j = 0; j < "CDHS".split("").length; j++) {
                    deck.push(i + "" + "CDHS".split("")[j]);
                }
            }
            for (int i = 0; i < "JQKA".split("").length; i++) {
                for (int j = 0; j < "CDHS".split("").length; j++) {
                    deck.push("JQKA".split("")[i] + "" + "CDHS".split("")[j]);
                }
            }
        }
        Collections.shuffle(deck);
    }

}
