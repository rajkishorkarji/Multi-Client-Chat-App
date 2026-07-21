package com.chatapp.server;

import com.chatapp.model.Message;
import com.chatapp.util.LoggerUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer {
    private static final Logger logger = LoggerUtil.getLogger(ChatServer.class);
    private final int port;
    private final ChatHistoryManager historyManager = new ChatHistorymanager();
    private final Map<String, ChatHandler> activeUsers = new ConcurrentHashMap<>();
    private final AtomicBoolean AdminAssigned = new AtomicBoolean(false);

    public ChatServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) {
        int port = 5000; // Default port
        if (args.length > 0) {
           
                port = Integer.parseInt(args[0]);
        }
        new ChatServer(port).start();
    }

     public void start() {
        historyManager.loadFromFile();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Chat server started on port {}", port);
            while (true) {
                Socket socket = serverSocket.accept();
                ChatHandler handler = new ChatHandler(socket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage(), e);
        }
    }

    public synchronized boolean assignAdminIfAvailable(String username, ChatHandler handler) {
        if (!adminAssigned.get()) {
            adminAssigned.set(true);
            activeUsers.put(username, handler);
            return true;
        }
        if (activeUsers.containsKey(username)) {
            return false;
        }
        activeUsers.put(username, handler);
        return false;
    }

    public void removeUser(String username) {
        activeUsers.remove(username);
    }

    public boolean isOnline(String username) {
        return activeUsers.containsKey(username);
    }

    public ChatHandler getUserHandler(String username) {
        return activeUsers.get(username);
    }

    public Map<String, ChatHandler> getActiveUsers() {
        return activeUsers;
    }

    public ChatHistoryManager getHistoryManager() {
        return historyManager;
    }

    public void broadcast(Message message) {
        historyManager.add(message);
        for (ChatHandler handler : activeUsers.values()) {
            handler.send(message.toString());
        }
    }

    public void broadcastSystem(String text) {
        broadcast(new Message("SYSTEM", text, true));
    }
    
}
