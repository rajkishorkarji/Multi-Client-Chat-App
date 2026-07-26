package com.chatapp.model;

import com.chatapp.util.EmojiUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final String sender;
    private final String text;
    private final LocalDateTime time;

    public Message(String sender, String text) {
        this.sender = sender;
        this.text = EmojiUtil.replaceShortcodes(text);
        this.time = LocalDateTime.now();
    }

    public String formatForRoom() {
        return "[Message] " + sender + " (" + time.format(FORMATTER) + "): " + text;
    }

    public String formatForSender() {
        return "[Message] You (" + time.format(FORMATTER) + "): " + text;
    }
}
