package com.example.blackjack_server;

import com.example.blackjack_server.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

public class Server {

    private ArrayList<String> serverCards = new ArrayList<>();
    private DatagramSocket socket;
    private ArrayList<Client> clients = new ArrayList<>();
    private Stack<String> cards = new Stack<>();


    public Server() {
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

    private void send(String s, int index){
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
            }
            if (m[0].equals("s")){
                serverCards.add(m[1]);
            }
            if (m[0].equals("k")){
                String card = cards.pop();
                clients.get(search(ip)).cards.add(card);
                send(card, search(ip));
            }
        }else{
            if (message.equals("exit")){
                send("paid:" + clients.get(search(ip)).money, search(ip));
            }
            if (message.equals("hit")){
                send("paid:" + clients.get(search(ip)).money, search(ip));
            }
            if (message.equals("stand")){
                send("paid:" + clients.get(search(ip)).money, search(ip));
            }
        }


    }

    private int search(String ip){
        int k = 0;
        while (!ip.equals(clients.get(k).ip)) k++;
        return k;
    }


}
