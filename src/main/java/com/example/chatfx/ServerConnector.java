package com.example.chatfx;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerConnector {
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

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
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
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
        System.out.println(message);
        message += '\n';
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            byteStream.write(message.getBytes(StandardCharsets.UTF_8));

            outputStream.write(byteStream.toByteArray());
            outputStream.flush();
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
