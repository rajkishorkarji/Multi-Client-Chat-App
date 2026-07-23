package com.chatapp.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class LoggerUtil {
    private LoggerUtil() {
    }

    public static Logger getLogger(Class<?> type) {
        Logger logger = Logger.getLogger(type.getName());
        logger.setUseParentHandlers(false);

        if (logger.getHandlers().length == 0) {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.INFO);
            logger.addHandler(handler);
        }

        logger.setLevel(Level.INFO);
        return logger;
    }
}
