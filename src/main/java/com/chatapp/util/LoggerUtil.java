package com.chatapp.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LoggerUtil {
    private static final Logger LOGGER = Logger.getLogger("ChatAppLogger");

    static {
        try {
            FileHandler fileHandler = new FileHandler("chat-app.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(true);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to initialize file logger", exception);
        }
    }

    private LoggerUtil() {
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
