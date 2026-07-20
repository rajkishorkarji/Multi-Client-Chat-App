package com.chatapp.server;

import com.chatapp.model.Message;
import com.chatapp.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = LoggerUtil.getLogger();

    private final Socket socket;
    private final ChatServer server;
    private PrintWriter writer;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)
        ) {
            writer = output;
            writer.println("Enter your username:");
            username = reader.readLine();

            if (username == null || username.isBlank()) {
                username = "Guest";
            }

            String joinMessage = username + " joined the chat.";
            System.out.println(joinMessage);
            LOGGER.info(joinMessage);
            server.broadcast(joinMessage, this);

            String clientMessage;
            while ((clientMessage = reader.readLine()) != null) {
                if ("/exit".equalsIgnoreCase(clientMessage.trim())) {
                    break;
                }

                Message message = new Message(username, clientMessage);
                String formattedMessage = message.formatForChat();
                System.out.println(formattedMessage);
                LOGGER.info(formattedMessage);
                server.broadcast(formattedMessage, this);
            }
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Client connection error", exception);
        } finally {
            closeConnection();
        }
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    private void closeConnection() {
        server.removeClient(this);

        if (username != null) {
            String leaveMessage = username + " left the chat.";
            System.out.println(leaveMessage);
            LOGGER.info(leaveMessage);
            server.broadcast(leaveMessage, this);
        }

        try {
            socket.close();
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to close client socket", exception);
        }
    }
}
