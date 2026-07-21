package com.chatapp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;

        if (args.length > 0) {
            host = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true)) {

            Thread listener = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverInput.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException ignored) {
                }
            });
            listener.setDaemon(true);
            listener.start();

            String userInput;
            while ((userInput = console.readLine()) != null) {
                serverOutput.println(userInput);
                if ("/exit".equalsIgnoreCase(userInput.trim())) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
        }
    }
}