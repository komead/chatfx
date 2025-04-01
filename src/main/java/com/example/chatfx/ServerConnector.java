package com.example.chatfx;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

public class ServerConnector {
    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    private final String ip = "localhost";
    private final int port = 9090;
    private boolean authorized = false;
    private String username;

    private static class ServerConnectorHolder {
        private static final ServerConnector instance = new ServerConnector();
    }

    public static ServerConnector getInstance() {
        return ServerConnectorHolder.instance;
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
            if (inputStream != null)
                inputStream.close();

            if (outputStream != null)
                outputStream.close();

            if (socket != null)
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

    public boolean isConnected() throws ConnectException {
        if (socket == null)
            throw new NullPointerException("Сервер недоступен");

        if (socket.isClosed())
            throw new ConnectException("Нет соединения с сервером");

        return true;
    }

    public void reconnect() throws IOException {
        finish();
        connect();
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
