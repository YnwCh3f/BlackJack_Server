package com.example.blackjack_server;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class HelloController {

    @FXML private Button btStart;
    @FXML private ListView<String> lvList;
    private DatagramSocket socket;
    private Server server = new Server();
    private boolean isStarted = false;

    private int bet = 0;
    private int index = 0;

    public void initialize(){
        while (server.sumCards < 17){
            String card = server.deck.pop();
            server.cards.add(card);
            // System.out.println(card);
            String[] a = card.split("");
            int value;
            if (a.length == 3) {
                value = 10;
            } else {
                if (server.royals.contains(a[0])) value = 10;
                else if (a[0].equals("A")) {
                    if (server.sumCards + 11 > 21) value = 1;
                    else value = 11;
                } else value = Integer.parseInt(a[0]);
            }
            server.sumCards += value;
        }
        if (server.sumCards > 21) server.full = true;
        //System.out.println(server.sumCards);
        try {
            socket = new DatagramSocket(678);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                receive();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void send(String s, int index){
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        try {
            InetAddress ipv4 = Inet4Address.getByName(server.clients.get(index).ip);
            int p = server.clients.get(index).port;
            DatagramPacket packet = new DatagramPacket(data, data.length, ipv4, p);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receive(){
        byte[] data = new byte[256];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        while (true) {
            try {
                socket.receive(packet);
                String message = new String(data, 0, packet.getLength());
                String ip = packet.getAddress().getHostAddress();
                int port = packet.getPort();
                Platform.runLater(() -> onReceive(message, ip, port));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void onReceive(String message, String ip, int port){
        String[] m = message.split(":");
        //if (server.clients.size() > 5) ;
        if (m.length > 1){
            if (m[0].equals("join") && server.joinable && Integer.parseInt(m[1]) >= 300){
                if (server.clients.size() == 0) btStart.setTextFill(Color.web("#ffa500"));
                Client c = new Client();
                c.ip = ip;
                c.port = port;
                int z = Integer.parseInt(m[1]);
                c.money = z;
                if (!lvList.getItems().contains(ip)){
                    server.clients.add(c);
                    lvList.getItems().add(ip);
                }
                send("joined:" + z, search(ip));
            }
            if (m[0].equals("bet")) {
                if (isStarted){
                if (server.clients.get(search(ip)).first) {
                    int x = Integer.parseInt(m[1]);
                    server.clients.get(search(ip)).money -= x;
                    server.clients.get(search(ip)).spent += x;
                    /*bet++;
                    if (bet == lvList.getItems().size()) {
                        index++;
                        bet = 0;
                    }*/
                    String card1 = server.deck.pop();
                    String[] a1 = card1.split("");
                    //System.out.println(card);
                    int value1;
                    if (a1.length == 3) {
                        value1 = 10;
                    } else {
                        if (server.royals.contains(a1[0])) value1 = 10;
                        else if (a1[0].equals("A")) {
                            if (server.clients.get(search(ip)).sumCards + 11 > 21) value1 = 1;
                            else value1 = 11;
                        } else value1 = Integer.parseInt(a1[0]);
                    }
                    server.clients.get(search(ip)).sumCards += value1;
                    String card2 = server.deck.pop();
                    String[] a2 = card2.split("");
                    //System.out.println(card);
                    int value2;
                    if (a2.length == 3) {
                        value2 = 10;
                    } else {
                        if (server.royals.contains(a2[0])) value2 = 10;
                        else if (a2[0].equals("A")) {
                            if (server.clients.get(search(ip)).sumCards + 11 > 21) value2 = 1;
                            else value2 = 11;
                        } else value2 = Integer.parseInt(a2[0]);
                    }
                    server.clients.get(search(ip)).sumCards += value2;
                    String card = server.cards.get(0);
                    send("s:" + card, search(ip));
                    send("k:" + card1, search(ip));
                    send("k:" + card2, search(ip));
                    server.clients.get(search(ip)).first = false;
                } else {
                    int x = Integer.parseInt(m[1]);
                    server.clients.get(search(ip)).money -= x;
                    server.clients.get(search(ip)).spent += x;
                }
            }
            }
        }else{
            if (message.equals("exit")){
                ObservableList<String> items = lvList.getItems();
                if (items.contains(ip)) {
                    items.remove(items.indexOf(ip));
                    send("paid:" + server.clients.get(search(ip)).money, search(ip));
                    server.clients.remove(search(ip));
                    if (server.clients.size() == 0){
                        btStart.setTextFill(Color.web("#ff0000"));
                        btStart.setText("START");
                        server.joinable = true;
                    }
                    isStarted = false;
                }

            }
            if (message.equals("hit")) {
                if (isStarted && !server.clients.get(search(ip)).isStand) {
                    if (lvList.getItems().contains(ip)) {
                        if (server.clients.get(search(ip)).sumCards < 21) {
                            String card = server.deck.pop();
                            String[] a = card.split("");
                            //System.out.println(card);
                            int value = 0;
                            if (a.length == 3) {
                                value = 10;
                            } else {
                                if (server.royals.contains(a[0])) value = 10;
                                else if (a[0].equals("A")) {
                                    if (server.clients.get(search(ip)).sumCards + 11 > 21) value = 1;
                                    else value = 11;
                                } else value = Integer.parseInt(a[0]);
                            }
                            server.clients.get(search(ip)).sumCards += value;
                            if (server.clients.get(search(ip)).sumCards > 21) server.clients.get(search(ip)).full = true;
                            //System.out.println(server.clients.get(search(ip)).sumCards);
                            send("k:" + card, search(ip));
                        }
                    }
                }
            }
            if (message.equals("stand")){
                server.clients.get(search(ip)).isStand = true;
                System.out.println(allStand());
                System.out.println(server.clients.get(search(ip)).sumCards);
                System.out.println(server.sumCards);
                if (allStand()){
                    for (Client c : server.clients) {
                        for (int i = 1; i < server.cards.size(); i++) {
                            send("s:" + server.cards.get(i), search(c.ip));
                        }
                    }
                    if (server.full){
                        for (Client c : server.clients){
                            if (!c.full){
                                server.clients.get(search(c.ip)).money += server.clients.get(search(c.ip)).spent*2;
                                send("balance:" + server.clients.get(search(c.ip)).money, search(c.ip));
                            }
                        }
                    }else{
                        for (Client c : server.clients){
                            if (!c.full && server.clients.get(search(c.ip)).sumCards > server.sumCards){
                                if (server.clients.get(search(c.ip)).cards.size() == 2){
                                    server.clients.get(search(c.ip)).money += server.clients.get(search(c.ip)).spent*2.5;
                                    send("balance:" + server.clients.get(search(c.ip)).money, search(c.ip));
                                }
                                else{
                                    server.clients.get(search(c.ip)).money += server.clients.get(search(c.ip)).spent*2;
                                    send("balance:" + server.clients.get(search(c.ip)).money, search(c.ip));
                                }
                            }
                            if (!c.full && server.clients.get(search(c.ip)).sumCards == server.sumCards){
                                server.clients.get(search(c.ip)).money += server.clients.get(search(c.ip)).spent;
                                send("balance:" + server.clients.get(search(c.ip)).money, search(c.ip));
                            }
                        }
                    }
                    server.joinable = true;
                    isStarted = false;
                    btStart.setTextFill(Color.web("#ffa500"));
                    btStart.setText("START");
                    server.full = false;
                    server.sumCards = 0;
                    server.cards.clear();
                    while (server.sumCards < 17){
                        String card = server.deck.pop();
                        server.cards.add(card);
                        // System.out.println(card);
                        String[] a = card.split("");
                        int value;
                        if (a.length == 3) {
                            value = 10;
                        } else {
                            if (server.royals.contains(a[0])) value = 10;
                            else if (a[0].equals("A")) {
                                if (server.sumCards + 11 > 21) value = 1;
                                else value = 11;
                            } else value = Integer.parseInt(a[0]);
                        }
                        server.sumCards += value;
                    }
                    for (Client c : server.clients){
                        c.first = true;
                        c.sumCards = 0;
                        c.cards.clear();
                        c.spent = 0;
                        c.full = false;
                        c.isStand = false;
                        //send("paid:" + server.clients.get(search(c.ip)).money, search(c.ip));
                    }
                }
            }
        }
    }

    private int search(String ip){
        int k = 0;
        while (!ip.equals(server.clients.get(k).ip)) k++;
        return k;
    }

    private boolean allStand(){
        int k = 0;
        int x = 0;
        while (x < server.clients.size()){
            if (server.clients.get(k).isStand) k++;
            x++;
        }
        return k == server.clients.size();
    }

    @FXML private void onStartClick() {
        if (server.clients.size() > 0) {
            server.joinable = false;
            btStart.setTextFill(Color.web("#00ff00"));
            btStart.setText("STARTED");
            isStarted = true;
            for (Client x : server.clients) {
                send("start:" + server.clients.size(), server.clients.indexOf(x));
            }
        }
    }

    private void valueOf(String card, String ip){
        String[] a = card.split("");
        //System.out.println(card);
        int value = 0;
        if (a.length == 3) {
            value = 10;
        } else {
            if (server.royals.contains(a[0])) value = 10;
            else if (a[0].equals("A")) {
                if (server.clients.get(search(ip)).sumCards + 11 > 21) value = 1;
                else value = 11;
            } else value = Integer.parseInt(a[0]);
        }
        server.clients.get(search(ip)).sumCards += value;
        if (server.clients.get(search(ip)).sumCards > 21) server.clients.get(search(ip)).full = true;
    }
}