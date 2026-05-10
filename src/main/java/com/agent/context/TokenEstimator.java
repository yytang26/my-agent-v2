package com.agent.context;

import com.agent.memory.Message;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenEstimator {

    public int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseCount = 0;
        int otherCount = 0;

        for (char c : text.toCharArray()) {
            if (isCjk(c)) {
                chineseCount++;
            } else {
                otherCount++;
            }
        }

        if (chineseCount == 0) {
            return Math.max(1, otherCount / 4);
        }
        if (otherCount == 0) {
            return Math.max(1, (int) Math.ceil(chineseCount / 1.5));
        }

        return Math.max(1, text.length() / 3);
    }

    public int estimate(Message message) {
        if (message == null) {
            return 0;
        }
        return estimate(message.content());
    }

    public int estimate(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (Message msg : messages) {
            total += estimate(msg);
        }
        return total;
    }

    private boolean isCjk(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }
}
