package com.chatapp.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class ChatHistoryManager {
    private final Path historyFile;

    public ChatHistoryManager(String fileName) {
        this.historyFile = Path.of(fileName);
        createFileIfMissing();
    }

    public synchronized void save(String message) {
        try {
            Files.writeString(
                    historyFile,
                    message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            System.out.println("[System] Unable to save chat history: " + ex.getMessage());
        }
    }

    public synchronized List<String> readAll() {
        try {
            if (!Files.exists(historyFile)) {
                return Collections.emptyList();
            }
            return Files.readAllLines(historyFile, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return Collections.emptyList();
        }
    }

    private void createFileIfMissing() {
        try {
            if (!Files.exists(historyFile)) {
                Files.createFile(historyFile);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create history file: " + historyFile, ex);
        }
    }
}
