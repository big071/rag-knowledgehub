package com.rag.knowledgehub;

import com.rag.knowledgehub.util.TextChunker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class TextChunkerTests {

    @Test
    void shouldChunkText() {
        String input = "a".repeat(1200);
        List<String> chunks = TextChunker.chunk(input, 400, 100);
        Assertions.assertFalse(chunks.isEmpty());
        Assertions.assertTrue(chunks.get(0).length() <= 400);
    }
}
