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

    private ArrayList<String> serverCards = new ArrayList<>();
    private DatagramSocket socket;
    public ArrayList<Client> clients = new ArrayList<>();
    private Stack<String> cards = new Stack<>();
    private HashSet<String> royals = new HashSet<>();
    private int sumServer = 0;

    public Server() {
        royals.add("J");
        royals.add("Q");
        royals.add("K");
        royals.add("A");
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
        for(int x = 0; x < 6; x++) {
            for (int i = 2; i < 11; i++) {
                for (int j = 0; j < "CDHS".split("").length; j++) {
                    cards.push(i + "" + "CDHS".split("")[j]);
                }
            }
            for (int i = 0; i < "JQKA".split("").length; i++) {
                for (int j = 0; j < "CDHS".split("").length; j++) {
                    cards.push("JQKA".split("")[i] + "" + "CDHS".split("")[j]);
                }
            }
        }
        Collections.shuffle(cards);
    }

    public void send(String s, int index){
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        try {
            InetAddress ipv4 = Inet4Address.getByName(clients.get(index).ip);
            int p = clients.get(index).port;
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
        if (clients.size() > 5) ;
        if (m.length > 1){
            if (m[0].equals("join")){
                Client c = new Client();
                c.ip = ip;
                c.port = port;
                int z = Integer.parseInt(m[1]);
                c.money = z;
                clients.add(c);
                send("joined:" + z, search(ip));
            }
            if (m[0].equals("bet")){
                int x = Integer.parseInt(m[1]);
                clients.get(search(ip)).money -= x;
                String card = cards.pop();
                serverCards.add(card);
                for (Client c : clients){
                    send("s:" + card, clients.indexOf(c));
                    send("s:gray_back", clients.indexOf(c));
                    send("k:" + cards.pop(), clients.indexOf(c));
                    send("k:" + cards.pop(), clients.indexOf(c));
                }
            }
        }else{
            if (message.equals("exit")){
                send("paid:" + clients.get(search(ip)).money, search(ip));
            }
            if (message.equals("hit")){
                send("k:" + cards.pop(), search(ip));
            }
            if (message.equals("stand")){
                clients.get(search(ip)).isStand = true;
            }
        }
        if (allStand()){
            if (sumServer < 17){
                String card = cards.pop();
                serverCards.add(card);
                String[] c = card.split("");
                if (royals.contains(c[0])){
                    if (c[0].equals("A")){
                        if (sumServer + 11 > 21) sumServer += 1;
                        else sumServer += 11;
                    } else sumServer += 10;
                }
            }
        }
    }

    private int search(String ip){
        int k = 0;
        while (!ip.equals(clients.get(k).ip)) k++;
        return k;
    }

    private boolean allStand(){
        int k = 0;
        int x = 0;
        while (x < clients.size()){
            if (clients.get(k).isStand) k++;
            x++;
        }
        return k == clients.size();
    }

}
