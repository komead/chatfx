package com.example.chatfx;

import com.example.chatfx.enums.OperationCode;
import com.google.gson.Gson;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class ServerConnector {
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private final String ip = "localhost";
    private final int port = 9090;
    private boolean authorized = false;
    private boolean connected = false;
    private String username = "";

    private static class ServerConnectorHolder {
        private static final ServerConnector instance = new ServerConnector();
    }

    public static ServerConnector getInstance() {
        return ServerConnectorHolder.instance;
    }

    public void connect() throws IOException {
        socket = new Socket(ip, port);
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
        connected = true;
    }

    public String checkMessage() throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        // Читаем полученную строку
        byte[] buffer = new byte[1024];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteStream.write(buffer, 0, bytesRead);

            if (bytesRead > 0 && buffer[bytesRead - 1] == '\n') {
                break;
            }
        }

        String receivedMessage = byteStream.toString(StandardCharsets.UTF_8).trim();

        System.out.println(receivedMessage);
        return receivedMessage;
    }

    public void sendMessage(String message) throws IOException {
        System.out.println(message);
        message += '\n';
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byteStream.write(message.getBytes(StandardCharsets.UTF_8));

        outputStream.write(byteStream.toByteArray());
        outputStream.flush();
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

    public void reconnect() throws IOException {
        finish();
        connect();

        HashMap<String, String> map = new HashMap<>();
        map.put("username", username);
        map.put("code", OperationCode.RECONNECT.stringValue());

        Gson gson = new Gson();
        sendMessage(gson.toJson(map));
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

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
