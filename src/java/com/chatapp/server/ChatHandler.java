package com.chatapp.server;

import com.chatapp.model.Message;
import com.chatapp.shared.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatHandler implements Runnable {
    private final ChatServer server;
    private final Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private String username;
    private boolean admin;
    private boolean muted;
    private boolean kickedByAdmin;
    private volatile boolean connected = true;

    public ChatHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            loginUser();

            String line;
            while (connected && (line = input.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                Command command = Command.fromInput(line);
                if (command == Command.UNKNOWN) {
                    sendChatMessage(line);
                } else {
                    handleCommand(command, Command.argumentFrom(line));
                }
            }
        } catch (IOException ex) {
            send("[System] Connection closed.");
        } finally {
            closeConnection();
        }
    }

    public String getUsername() {
        return username;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void send(String message) {
        if (output != null) {
            output.println(message);
        }
    }

    public void disconnect(String reason) {
        send(reason);
        connected = false;
        closeConnection();
    }

    public void kickOut(String reason) {
        kickedByAdmin = true;
        disconnect(reason);
    }

    private void loginUser() throws IOException {
        send("[System] Enter username.");

        while (true) {
            String requestedName = input.readLine();
            if (requestedName == null) {
                throw new IOException("Client disconnected before entering username.");
            }

            requestedName = requestedName.trim();
            if (requestedName.isEmpty()) {
                send("[System] Username cannot be empty. Enter username.");
                continue;
            }

            if (server.registerClient(requestedName, this)) {
                username = requestedName;
                admin = server.isFirstClient(this);
                joinPublicLobby();
                return;
            }

            send("[System] This username is already taken. Enter another username.");
        }
    }

    private void joinPublicLobby() {
        send("");
        send("[System] Welcome, " + username + ".");
        send("[System] You joined the public chat lobby.");

        if (admin) {
            send("[System] You are the room admin.");
            send("[System] Type \\help to see management commands.");
        } else {
            send("[System] Type \\help to see available commands.");
        }

        send("");
        System.out.println("[System] " + username + " joined the public lobby.");
        server.saveEvent(username + " joined the public lobby.");
        server.broadcastPublicExcept(this, "[System] " + username + " joined the public lobby.");
    }

    private void sendChatMessage(String text) {
        if (muted) {
            send("[System] You are muted by the admin. You can read messages but cannot send new messages.");
            return;
        }

        Message message = new Message(username, text);
        String formattedForRoom = message.formatForRoom();
        String formattedForSender = message.formatForSender();
        String roomCode = server.privateRoomCodeFor(this);

        if (roomCode == null) {
            System.out.println(formattedForRoom);
            server.getHistoryManager().save(formattedForRoom);
            send(formattedForSender);
            server.broadcastPublicExcept(this, formattedForRoom);
            return;
        }

        String privateMessage = "[Private " + roomCode + "] " + formattedForRoom;
        System.out.println(privateMessage);
        server.getHistoryManager().save(privateMessage);
        send("[Private " + roomCode + "] " + formattedForSender);
        server.broadcastPrivateRoomExcept(this, privateMessage);
    }

    private void handleCommand(Command command, String argument) {
        switch (command) {
            case HELP -> sendHelp();
            case USERS -> send(server.connectedUsers());
            case HISTORY -> sendHistory();
            case CREATEPRIVATE -> server.createPrivateRoom(this);
            case JOINPRIVATE -> server.joinPrivateRoom(argument, this);
            case LEAVE -> server.leavePrivateRoom(this, true);
            case ROOMCODE -> sendCurrentRoomCode();
            case EXIT -> disconnect("[System] You left the chat room. Goodbye.");
            case MUTE, UNMUTE, KICK -> handleAdminCommand(command, argument);
            default -> send("[System] Unknown command. Type \\help to see available commands.");
        }
    }

    private void handleAdminCommand(Command command, String argument) {
        if (!admin) {
            send("[System] This is an admin command. Type \\help to see available commands.");
            return;
        }

        switch (command) {
            case MUTE -> server.muteUser(argument, true, this);
            case UNMUTE -> server.muteUser(argument, false, this);
            case KICK -> server.kickUser(argument, this);
            default -> send("[System] Invalid management command.");
        }
    }

    private void sendHelp() {
        send("");

        if (admin) {
            send("Management commands:");
            send("  \\help              Show management commands");
            send("  \\users             Show online users in public/private rooms");
            send("  \\createprivate     Create a private room and generate a code");
            send("  \\joinprivate <code> Join a private room using a code");
            send("  \\leave             Leave private room and return to public lobby");
            send("  \\roomcode          Show your current private room code");
            send("  \\mute <username>   Mute a user in public/private chat");
            send("  \\unmute <username> Unmute a user");
            send("  \\kick <username>   Remove a user from the server");
            send("  \\history           Show full public/private history and events");
            send("  \\exit              Leave the chat room");
        } else {
            send("Available commands:");
            send("  \\help       Show available commands");
            send("  \\users      Show online users");
            send("  \\createprivate        Create a private room and generate a code");
            send("  \\joinprivate <code>   Join a private room using a code");
            send("  \\leave                Leave private room and return to public lobby");
            send("  \\roomcode   Show your current private room code");
            send("  \\history    Show previous public messages");
            send("  \\exit       Leave the chat room");
        }

        send("");
    }

    private void sendCurrentRoomCode() {
        String roomCode = server.privateRoomCodeFor(this);
        if (roomCode == null) {
            send("[System] You are in the public lobby. Use \\createprivate to generate a private room code.");
            return;
        }

        send("[System] Current private room code: " + roomCode);
    }

    private void sendHistory() {
        if (admin) {
            var history = server.getHistoryManager().readAll();
            if (history.isEmpty()) {
                send("[System] No history found.");
                return;
            }
            send("[System] Full history (public/private messages & events):");
            for (String line : history) {
                send(line);
            }
        } else {
            var history = server.getHistoryManager().readMessagesOnly();
            if (history.isEmpty()) {
                send("[System] No previous messages found.");
                return;
            }
            send("[System] Previous messages:");
            for (String line : history) {
                send(line);
            }
        }
    }

    private void closeConnection() {
        if (!connected && socket.isClosed()) {
            return;
        }

        connected = false;
        server.unregisterClient(this, !kickedByAdmin);

        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}

