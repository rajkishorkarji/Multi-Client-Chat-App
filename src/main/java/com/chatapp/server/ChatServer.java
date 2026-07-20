package com.chatapp.server;

import com.chatapp.util.LoggerUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatServer {
    private static final int PORT = 5000;
    private static final Logger LOGGER = LoggerUtil.getLogger();

    private final Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        LOGGER.info("Chat server is starting on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException exception) {
            LOGGER.log(Level.SEVERE, "Server stopped because of an error", exception);
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }
}
