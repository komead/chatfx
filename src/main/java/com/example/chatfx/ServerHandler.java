package com.example.chatfx;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerHandler {
    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    private final String ip = "localhost";
    private final int port = 9090;

    private static class ServerHandlerHolder {
        private static final ServerHandler instance = new ServerHandler();
    }

    public static ServerHandler getInstance() {
        return ServerHandlerHolder.instance;
    }

    public void connect() {
        try {
            socket = new Socket(ip, port);
            inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String checkMessage() {
        String receivedMessage = "";
        try {
            receivedMessage = inputStream.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return receivedMessage;
    }

    public void finish() {
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            outputStream.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return socket.isConnected();
    }
}
