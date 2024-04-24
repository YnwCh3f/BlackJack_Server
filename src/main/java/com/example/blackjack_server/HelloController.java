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

    public void initialize(){
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
        if (server.clients.size() > 5) ;
        if (m.length > 1){
            if (m[0].equals("join")){
                if (server.clients.size() == 0) btStart.setTextFill(Color.web("#00ff00"));
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
            if (m[0].equals("bet")){
                int x = Integer.parseInt(m[1]);
                //if (server.clients.get(search(ip)).money == x) return;
                server.clients.get(search(ip)).money -= x;
                    String card = server.deck.pop();
                    server.cards.add(card);
                    send("s:" + card, search(ip));
                    send("k:" + server.deck.pop(), search(ip));
                    send("k:" + server.deck.pop(), search(ip));
            }
        }else{
            if (message.equals("exit")){
                ObservableList<String> items = lvList.getItems();
                if (items.contains(ip)) {
                    items.remove(items.indexOf(ip));
                    send("paid:" + server.clients.get(search(ip)).money, search(ip));
                    server.clients.remove(search(ip));
                    if (server.clients.size() == 0) btStart.setTextFill(Color.web("#ff0000"));
                    isStarted = false;
                    //System.out.println();
                }

            }
            if (message.equals("hit")) {
                if (isStarted) {
                    if (lvList.getItems().contains(ip)) {
                        if (server.clients.get(search(ip)).sumCards < 21) {
                            String card = server.deck.pop();
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
                            send("k:" + card, search(ip));
                            System.out.println(server.clients.get(search(ip)).sumCards + "  " + card);
                        }
                    }
                }
            }
            if (message.equals("stand")){
                server.clients.get(search(ip)).isStand = true;
            }
        }
        if (allStand()){

            server.cards.add(server.deck.pop());
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
            isStarted = true;
            for (Client x : server.clients) {
                send("start:" + server.clients.size(), server.clients.indexOf(x));
            }
        }
    }
}