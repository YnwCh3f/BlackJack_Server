package com.example.blackjack_server;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

public class HelloController {

    @FXML private Button btStart;
    @FXML private ListView<String> lvList;
    private DatagramSocket socket;
    private Server server = new Server();
    private boolean isStarted = false;
    private ArrayList<String> used;

    private AnimationTimer atTimer = null;
    private long time = 0;
    private int x = 30;

    public void initialize(){
        used = new ArrayList<>();
        while (server.sumCards < 17){
            String card = server.deck.pop();
            used.add(card);
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
        atTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now > time){
                    time = now + 1_000_000_000;
                    btStart.setText("START (" + x + ")");
                    x--;
                    if (x == -1) onStartClick();
                }
            }
        };
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
        if (m.length > 1){
            if (m[0].equals("join") && server.joinable && Integer.parseInt(m[1]) >= 300){
                x = 30;
                atTimer.start();
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
                    String card1 = server.deck.pop();
                    valueOf(card1, ip);
                    String card2 = server.deck.pop();
                   valueOf(card2, ip);
                    String card = server.cards.get(0);
                    used.add(card1);
                    used.add(card2);
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
                if (!isStarted) {
                    if (items.contains(ip)) {
                        items.remove(items.indexOf(ip));
                        send("paid:" + server.clients.get(search(ip)).money, search(ip));
                        server.clients.remove(search(ip));
                        if (server.clients.size() == 0) {
                            btStart.setTextFill(Color.web("#ff0000"));
                            atTimer.stop();
                            btStart.setText("START");
                            server.joinable = true;
                        }
                        isStarted = false;
                    }
                }
            }
            if (message.equals("hit")) {
                if (isStarted && !server.clients.get(search(ip)).isStand) {
                    if (lvList.getItems().contains(ip)) {
                        if (server.clients.get(search(ip)).sumCards < 21) {
                            String card = server.deck.pop();
                            used.add(card);
                            valueOf(card, ip);
                            if (server.clients.get(search(ip)).sumCards > 21) server.clients.get(search(ip)).full = true;
                            send("k:" + card, search(ip));
                        }
                    }
                }
            }
            if (message.equals("stand")){
                server.clients.get(search(ip)).isStand = true;
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
                            }
                            send("balance:" + server.clients.get(search(c.ip)).money, search(c.ip));
                        }
                    }else{
                        for (Client c : server.clients){
                            if (!c.full && server.clients.get(search(c.ip)).sumCards > server.sumCards){
                                if (server.clients.get(search(c.ip)).cards.size() == 2){
                                    server.clients.get(search(c.ip)).money += server.clients.get(search(c.ip)).spent*2.5;
                                }
                                else{
                                    server.clients.get(search(c.ip)).money += server.clients.get(search(c.ip)).spent*2;
                                }
                                send("balance:" + server.clients.get(search(c.ip)).money, search(c.ip));
                            }
                            if (!c.full && server.clients.get(search(c.ip)).sumCards == server.sumCards){
                                server.clients.get(search(c.ip)).money += server.clients.get(search(c.ip)).spent;
                            }
                            send("balance:" + server.clients.get(search(c.ip)).money, search(c.ip));
                        }
                    }
                    for (String x : used){
                        server.deck.push(x);
                    }
                    used.clear();
                    Collections.shuffle(server.deck);
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
                        used.add(card);
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
                    for (Client c : server.clients){
                        c.first = true;
                        c.sumCards = 0;
                        c.cards.clear();
                        c.spent = 0;
                        c.full = false;
                        c.isStand = false;
                    }
                    time = 0;
                    x = 30;
                    atTimer.start();
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
            atTimer.stop();
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
    }
}