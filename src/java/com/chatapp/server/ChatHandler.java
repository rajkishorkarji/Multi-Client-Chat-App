package com.chatapp.server;

import com.chatapp.model.Message;
import com.chatapp.shared.Command;
import com.chatapp.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ChatHandler implements Runnable {
    private static final Logger logger = LoggerUtil.getLogger(ChatHandler.class);

    private final Socket socket;
    private final ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private boolean admin;
    private volatile boolean running = true;
    private volatile boolean muted = false;

    public ChatHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            writer.println("Enter username:");
            username = reader.readLine();

            if (username == null || username.isBlank()) {
                close();
                return;
            }

            admin = server.assignAdminIfAvailable(username, this);

            if (admin) {
                writer.println("You are the ADMIN of this room.");
                server.broadcastSystem(username + " joined as ADMIN.");
            } else {
                writer.println("You joined as a client.");
                server.broadcastSystem(username + " joined the room.");
            }

            sendHelp();

            String line;
            while (running && (line = reader.readLine()) != null) {
                handleInput(line.trim());
            }
        } catch (IOException e) {
            logger.error("Client handler error: {}", e.getMessage());
        } finally {
            server.removeUser(username);
            server.broadcastSystem(username + " disconnected.");
            close();
        }
    }

    private void handleInput(String input) {
        if (input.isBlank()) {
            return;
        }

        if (input.startsWith("/")) {
            handleCommand(input);
            return;
        }

        if (muted) {
            writer.println("You are muted and cannot send messages.");
            return;
        }

        server.broadcast(new Message(username, input, false));
    }

    private void handleCommand(String input) {
        String[] parts = input.split("\\s+", 3);
        Command command = parseCommand(parts[0]);

        switch (command) {
            case HELP -> sendHelp();
            case USERS -> sendUsers();
            case HISTORY -> sendHistory();
            case EXIT -> {
                writer.println("Goodbye.");
                running = false;
            }
            case MUTE -> {
                if (!admin) {
                    writer.println("Only admin can mute users.");
                    return;
                }
                if (parts.length < 2) {
                    writer.println("Usage: /mute <username>");
                    return;
                }
                muteUser(parts[1]);
            }
            case UNMUTE -> {
                if (!admin) {
                    writer.println("Only admin can unmute users.");
                    return;
                }
                if (parts.length < 2) {
                    writer.println("Usage: /unmute <username>");
                    return;
                }
                unmuteUser(parts[1]);
            }
            case KICK -> {
                if (!admin) {
                    writer.println("Only admin can kick users.");
                    return;
                }
                if (parts.length < 2) {
                    writer.println("Usage: /kick <username>");
                    return;
                }
                kickUser(parts[1]);
            }
            case DELETE -> {
                if (!admin) {
                    writer.println("Only admin can delete history.");
                    return;
                }
                if (parts.length < 2) {
                    writer.println("Usage: /delete <index>");
                    return;
                }
                deleteMessage(parts[1]);
            }
            default -> writer.println("Unknown command. Type /help");
        }
    }

    private Command parseCommand(String raw) {
        return switch (raw.toLowerCase()) {
            case "/help" -> Command.HELP;
            case "/users" -> Command.USERS;
            case "/mute" -> Command.MUTE;
            case "/unmute" -> Command.UNMUTE;
            case "/kick" -> Command.KICK;
            case "/delete" -> Command.DELETE;
            case "/history" -> Command.HISTORY;
            case "/exit" -> Command.EXIT;
            default -> Command.UNKNOWN;
        };
    }

    private void sendHelp() {
        writer.println("Commands:");
        writer.println("/help");
        writer.println("/users");
        writer.println("/history");
        writer.println("/exit");
        writer.println("Admin only:");
        writer.println("/mute <username>");
        writer.println("/unmute <username>");
        writer.println("/kick <username>");
        writer.println("/delete <index>");
    }

    private void sendUsers() {
        writer.println("Online users:");
        for (Map.Entry<String, ChatHandler> entry : server.getActiveUsers().entrySet()) {
            writer.println("- " + entry.getKey() + (entry.getValue().admin ? " [ADMIN]" : ""));
        }
    }

    private void sendHistory() {
        List<Message> messages = server.getHistoryManager().getAll();
        if (messages.isEmpty()) {
            writer.println("No history yet.");
            return;
        }
        for (int i = 0; i < messages.size(); i++) {
            writer.println(i + ": " + messages.get(i));
        }
    }

    private void muteUser(String target) {
        ChatHandler handler = server.getUserHandler(target);
        if (handler == null) {
            writer.println("User not found: " + target);
            return;
        }
        handler.muted = true;
        handler.send("You have been muted by admin.");
        server.broadcastSystem(target + " was muted by admin.");
    }

    private void unmuteUser(String target) {
        ChatHandler handler = server.getUserHandler(target);
        if (handler == null) {
            writer.println("User not found: " + target);
            return;
        }
        handler.muted = false;
        handler.send("You have been unmuted by admin.");
        server.broadcastSystem(target + " was unmuted by admin.");
    }

    private void kickUser(String target) {
        ChatHandler handler = server.getUserHandler(target);
        if (handler == null) {
            writer.println("User not found: " + target);
            return;
        }
        handler.send("You have been kicked by admin.");
        handler.running = false;
        handler.close();
        server.removeUser(target);
        server.broadcastSystem(target + " was kicked by admin.");
    }

    private void deleteMessage(String indexText) {
        try {
            int index = Integer.parseInt(indexText);
            boolean deleted = server.getHistoryManager().deleteByIndex(index);
            if (deleted) {
                writer.println("Message deleted from history.");
                server.broadcastSystem("Admin deleted message at index " + index);
            } else {
                writer.println("Invalid message index.");
            }
        } catch (NumberFormatException e) {
            writer.println("Index must be a number.");
        }
    }

    public void send(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    private void close() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }
        if (writer != null) {
            writer.close();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}