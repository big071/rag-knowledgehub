package com.rag.knowledgehub.util;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> chunk(String text, int chunkSize, int overlap) {
        String clean = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        List<String> chunks = new ArrayList<>();
        if (clean.isEmpty()) {
            return chunks;
        }
        int step = Math.max(1, chunkSize - overlap);
        for (int start = 0; start < clean.length(); start += step) {
            int end = Math.min(clean.length(), start + chunkSize);
            chunks.add(clean.substring(start, end));
            if (end == clean.length()) {
                break;
            }
        }
        return chunks;
    }
}
