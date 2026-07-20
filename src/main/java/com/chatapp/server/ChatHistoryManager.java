package com.chatapp.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class ChatHistoryManager {
    private static final Path HISTORY_FILE = Path.of("chat.txt");

    public void saveMessage(String message) throws IOException {
        Files.writeString(
                HISTORY_FILE,
                message + System.lineSeparator(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    public List<String> readHistory() throws IOException {
        if (Files.notExists(HISTORY_FILE)) {
            return List.of();
        }

        return Files.readAllLines(HISTORY_FILE);
    }
}
