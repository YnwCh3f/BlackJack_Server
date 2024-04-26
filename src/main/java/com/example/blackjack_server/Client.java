package com.example.blackjack_server;

import java.util.ArrayList;

public class Client {

    public int port;
    public String ip;
    public int money;
    public ArrayList<String> cards = new ArrayList<>();
    public boolean isStand = false;
    public int sumCards = 0;
    public int spent = 0;
    public boolean full = false;
    public boolean first = true;

    public Client(){

    }

}
