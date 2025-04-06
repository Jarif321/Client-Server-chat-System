import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        int port = 12345;
        System.out.println("Server is running on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            clients.stream()
                   .filter(c -> c != sender)
                   .forEach(c -> c.sendMessage(message));
        }
    }

    private static void removeClient(ClientHandler client) {
        clients.remove(client);
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader input;
        private PrintWriter output;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = input.readLine()) != null) {
                    System.out.println("Received: " + message);
                    broadcast(message, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                removeClient(this);
                System.out.println("Client disconnected");
            }
        }

        public void sendMessage(String message) {
            output.println(message);
        }
    }
}
