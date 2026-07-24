package com.chatapp.server;

import com.chatapp.util.LoggerUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger LOGGER = LoggerUtil.getLogger(ChatServer.class);
    private static final int DEFAULT_PORT = 5000;
    private static final String DEFAULT_HISTORY_FILE = "chat.txt";
    private static final DateTimeFormatter EVENT_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final Map<String, ChatHandler> clients = new ConcurrentHashMap<>();
    private final List<ChatHandler> joinOrder = new ArrayList<>();
    private final ChatHistoryManager historyManager;

    public ChatServer(ChatHistoryManager historyManager) {
        this.historyManager = historyManager;
    }

    public static void main(String[] args) {
        Properties config = loadConfig();
        int port = Integer.parseInt(config.getProperty("server.port", String.valueOf(DEFAULT_PORT)));
        String historyFile = config.getProperty("chat.history.file", DEFAULT_HISTORY_FILE);

        ChatServer server = new ChatServer(new ChatHistoryManager(historyFile));
        server.start(port);
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[System] Multi Client Chat Server started on port " + port);
            System.out.println("[System] Waiting for clients to join...");
            System.out.println();

            while (true) {
                Socket socket = serverSocket.accept();
                ChatHandler handler = new ChatHandler(this, socket);
                new Thread(handler).start();
            }
        } catch (IOException ex) {
            LOGGER.severe("Server stopped: " + ex.getMessage());
        }
    }

    public ChatHistoryManager getHistoryManager() {
        return historyManager;
    }

    public synchronized boolean registerClient(String username, ChatHandler handler) {
        String key = username.toLowerCase();
        if (clients.containsKey(key)) {
            return false;
        }

        clients.put(key, handler);
        joinOrder.add(handler);
        return true;
    }

    public synchronized void unregisterClient(ChatHandler handler, boolean announceDeparture) {
        if (handler.getUsername() == null) {
            return;
        }

        boolean removed = clients.remove(handler.getUsername().toLowerCase()) != null;
        joinOrder.remove(handler);

        if (removed && announceDeparture) {
            String msg = "[System] " + handler.getUsername() + " left the room.";
            System.out.println(msg);
            saveEvent(handler.getUsername() + " left the room.");
            broadcast(msg);
        }

        if (removed && handler.isAdmin()) {
            assignNextAdmin();
        }
    }

    public synchronized boolean isFirstClient(ChatHandler handler) {
        return !joinOrder.isEmpty() && joinOrder.get(0) == handler;
    }

    public void broadcast(String message) {
        for (ChatHandler client : clients.values()) {
            client.send(message);
        }
    }

    public void broadcastExcept(ChatHandler excludedClient, String message) {
        for (ChatHandler client : clients.values()) {
            if (client != excludedClient) {
                client.send(message);
            }
        }
    }

    public String connectedUsers() {
        List<String> users = clients.values()
                .stream()
                .sorted(Comparator.comparing(ChatHandler::getUsername))
                .map(client -> client.getUsername() + (client.isAdmin() ? " (Admin)" : " (Client)"))
                .toList();

        return "[System] Online users: " + String.join(", ", users);
    }

    public void muteUser(String username, boolean muted, ChatHandler admin) {
        ChatHandler target = findUser(username, admin);
        if (target == null) {
            return;
        }

        if (target.isAdmin()) {
            admin.send("[System] Admin cannot be muted.");
            return;
        }

        target.setMuted(muted);
        String action = muted ? "muted" : "unmuted";
        System.out.println("[System] " + target.getUsername() + " has been " + action + ".");
        target.send("[System] You are " + action + " by the admin.");
        broadcastExcept(target, "[System] " + target.getUsername() + " has been " + action + ".");
        saveEvent(admin.getUsername() + " " + action + " " + target.getUsername() + ".");
    }

    public void kickUser(String username, ChatHandler admin) {
        ChatHandler target = findUser(username, admin);
        if (target == null) {
            return;
        }

        if (target.isAdmin()) {
            admin.send("[System] Admin cannot kick himself from the room.");
            return;
        }

        System.out.println("[System] " + target.getUsername() + " has been removed from the room by admin.");
        saveEvent(admin.getUsername() + " kicked " + target.getUsername() + " from the room.");
        broadcastExcept(target, "[System] " + target.getUsername() + " has been removed from the room by admin.");
        target.kickOut("[System] You are removed from the room by the admin.");
    }

    private ChatHandler findUser(String username, ChatHandler admin) {
        if (username == null || username.isBlank()) {
            admin.send("[System] Please enter a username. Example: \\mute Max");
            return null;
        }

        ChatHandler target = clients.get(username.toLowerCase());
        if (target == null) {
            admin.send("[System] No online user found with name: " + username);
        }
        return target;
    }

    private synchronized void assignNextAdmin() {
        if (joinOrder.isEmpty()) {
            return;
        }

        ChatHandler nextAdmin = joinOrder.get(0);
        nextAdmin.setAdmin(true);
        System.out.println("[System] " + nextAdmin.getUsername() + " is now the room admin.");
        nextAdmin.send("[System] You are now the admin of this room.");
        nextAdmin.send("[System] Type \\help to see management commands.");
        broadcastExcept(nextAdmin, "[System] " + nextAdmin.getUsername() + " is now the room admin.");
        saveEvent(nextAdmin.getUsername() + " is now the room admin.");
    }

    /** Persists a system event to the history file with timestamp and [System-Event] tag. */
    public void saveEvent(String eventDescription) {
        String timestamp = LocalDateTime.now().format(EVENT_FORMATTER);
        historyManager.save("[System-Event] (" + timestamp + "): " + eventDescription);
    }

    private static Properties loadConfig() {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("server.properties")) {
            properties.load(input);
        } catch (IOException ex) {
            properties.setProperty("server.port", String.valueOf(DEFAULT_PORT));
            properties.setProperty("chat.history.file", DEFAULT_HISTORY_FILE);
        }

        return properties;
    }
}
