package com.chatapp.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatClient {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5000;
    private static final String INPUT_PROMPT = "> ";

    public static void main(String[] args) {
        configureUtf8Console();

        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        new ChatClient().start(host, port);
    }

    public void start(String host, int port) {
        System.out.println("[System] Connecting to Multi Client Chat Room...");

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)
        ) {
            Thread listener = new Thread(() -> listenToServer(serverInput));
            listener.setDaemon(true);
            listener.start();

            while (!socket.isClosed()) {
                System.out.print(INPUT_PROMPT);
                String input = scanner.nextLine();
                serverOutput.println(input);

                if ("\\exit".equalsIgnoreCase(input.trim())) {
                    break;
                }
            }
        } catch (IOException ex) {
            System.out.println("[System] Unable to connect to server: " + ex.getMessage());
            System.out.println("[System] Please start ChatServer first, then open one or more ChatClient windows.");
        }
    }

    private void listenToServer(BufferedReader serverInput) {
        try {
            String message;
            while ((message = serverInput.readLine()) != null) {
                System.out.println(message);
            }
        } catch (IOException ex) {
            System.out.println("[System] Disconnected from chat room.");
        }
    }

    private static void configureUtf8Console() {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
