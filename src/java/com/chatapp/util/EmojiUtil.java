package com.chatapp.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EmojiUtil {
    private static final Map<String, String> EMOJIS = new LinkedHashMap<>();

    static {
        EMOJIS.put(":smile:", "\uD83D\uDE04");
        EMOJIS.put(":laugh:", "\uD83D\uDE02");
        EMOJIS.put(":heart:", "\u2764\uFE0F");
        EMOJIS.put(":thumbsup:", "\uD83D\uDC4D");
        EMOJIS.put(":clap:", "\uD83D\uDC4F");
        EMOJIS.put(":fire:", "\uD83D\uDD25");
        EMOJIS.put(":sad:", "\uD83D\uDE22");
        EMOJIS.put(":angry:", "\uD83D\uDE20");
        EMOJIS.put(":ok:", "\uD83D\uDC4C");
        EMOJIS.put(":party:", "\uD83C\uDF89");
        EMOJIS.put(":love:", "\uD83E\uDD70");
        EMOJIS.put(":cool:", "\uD83D\uDE0E");
        EMOJIS.put(":think:", "\uD83E\uDD14");
        EMOJIS.put(":pray:", "\uD83D\uDE4F");
        EMOJIS.put(":star:", "\u2B50");
        EMOJIS.put(":100:", "\uD83D\uDCAF");
        EMOJIS.put(":)", "\uD83D\uDE42");
        EMOJIS.put(":D", "\uD83D\uDE03");
        EMOJIS.put(":(", "\uD83D\uDE41");
        EMOJIS.put("<3", "\u2764\uFE0F");
    }

    private EmojiUtil() {
    }

    public static String replaceShortcodes(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String withEmoji = text;
        for (Map.Entry<String, String> emoji : EMOJIS.entrySet()) {
            withEmoji = withEmoji.replace(emoji.getKey(), emoji.getValue());
        }
        return withEmoji;
    }
}
