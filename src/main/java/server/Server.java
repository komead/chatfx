package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(9090)) {
            System.out.println("Ожидание подключения...");
            Socket socket = serverSocket.accept();
            System.out.println("Клиент подключён");
            ClientHandler clientHandler = new ClientHandler(socket);

            // В отдельном потоке слушаем клиента
            new Thread(() -> {
                String message = "";
                while (true) {
                    message = clientHandler.checkMessage();

                    // Если клиент отключается, то больше его не слушаем
                    if (message.equals("/end")) {
                        System.out.println("Клиент отключился!");
                        clientHandler.finish();
                        break;
                    }

                    System.out.println("client: " + message);
                }
            }).start();

            // В отдельном потоке читаем вводимые данные в консоли и отправляем их клиенту
            Thread thread = new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                while (scanner.hasNextLine()) {
                    clientHandler.sendMessage(scanner.nextLine());
                }
            });
            thread.setDaemon(true);
            thread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
