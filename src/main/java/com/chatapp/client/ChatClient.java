package com.chatapp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to chat server.");

            Thread receiverThread = new Thread(() -> receiveMessages(serverReader));
            receiverThread.setDaemon(true);
            receiverThread.start();

            String userInput;
            while ((userInput = keyboardReader.readLine()) != null) {
                serverWriter.println(userInput);

                if ("/exit".equalsIgnoreCase(userInput.trim())) {
                    break;
                }
            }
        } catch (IOException exception) {
            System.out.println("Unable to connect to the chat server: " + exception.getMessage());
        }
    }

    private static void receiveMessages(BufferedReader serverReader) {
        try {
            String message;
            while ((message = serverReader.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException exception) {
            System.out.println("Disconnected from server.");
        }
    }
}
