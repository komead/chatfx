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
    private boolean authorized = false;

    private static class ServerHandlerHolder {
        private static final ServerHandler instance = new ServerHandler();
    }

    public static ServerHandler getInstance() {
        return ServerHandlerHolder.instance;
    }

    public void connect() throws IOException {
        socket = new Socket(ip, port);
        inputStream = new DataInputStream(socket.getInputStream());
        outputStream = new DataOutputStream(socket.getOutputStream());
    }

    public String checkMessage() throws IOException {
        String receivedMessage = "";
        receivedMessage = inputStream.readUTF();

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
        return socket != null && socket.isConnected();
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }
}
