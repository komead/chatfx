package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Socket socket;
    private DataOutputStream outputStream;
    private DataInputStream inputStream;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            inputStream = new DataInputStream(this.socket.getInputStream());
            outputStream = new DataOutputStream(this.socket.getOutputStream());
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
}
