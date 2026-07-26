package com.chatapp.shared;

public enum Command {
    HELP,
    USERS,
    MUTE,
    UNMUTE,
    KICK,
    HISTORY,
    CREATEPRIVATE,
    JOINPRIVATE,
    LEAVE,
    ROOMCODE,
    EXIT,
    UNKNOWN;

    public static Command fromInput(String input) {
        if (input == null || input.isBlank()) {
            return UNKNOWN;
        }

        String commandName = input.trim().split("\\s+")[0];
        if (!commandName.startsWith("\\")) {
            return UNKNOWN;
        }

        commandName = commandName.substring(1);
        if (commandName.isBlank()) {
            return UNKNOWN;
        }

        for (Command command : values()) {
            if (command.name().equalsIgnoreCase(commandName)) {
                return command;
            }
        }
        return UNKNOWN;
    }

    public static String argumentFrom(String input) {
        if (input == null) {
            return "";
        }

        String[] parts = input.trim().split("\\s+", 2);
        return parts.length == 2 ? parts[1].trim() : "";
    }
}

