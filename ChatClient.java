import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public ChatClient(String serverAddress, int port) {
        try {
            connectToServer(serverAddress, port);
            startMessageReceiver();

            handleUserInput();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void connectToServer(String address, int port) throws IOException {
        socket = new Socket(address, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    private void startMessageReceiver() {
        Thread receiverThread = new Thread(new IncomingMessageHandler());
        receiverThread.start();
    }

    private void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String message = scanner.nextLine();
            writer.println(message);
        }
    }

    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class IncomingMessageHandler implements Runnable {
        @Override
        public void run() {
            try {
                String incomingMessage;
                while ((incomingMessage = reader.readLine()) != null) {
                    System.out.println(incomingMessage);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new ChatClient("localhost", 12345);
    }
}
