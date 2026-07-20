package com.chatapp.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;

    public Message(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String formatForChat() {
        return "[" + timestamp.format(FORMATTER) + "] " + sender + ": " + content;
    }
}
