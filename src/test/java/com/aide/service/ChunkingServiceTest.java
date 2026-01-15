package com.aide.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService(500, 50);

    @Test
    void chunkShouldReturnEmptyListForNullOrBlankInput() {
        assertThat(chunkingService.chunk(null)).isEmpty();
        assertThat(chunkingService.chunk("   ")).isEmpty();
    }

    @Test
    void chunkShouldReturnSingleChunkWhenContentIsShorterThanChunkSize() {
        String text = "short content";

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks).containsExactly(text);
    }

    @Test
    void chunkShouldCreateOverlappingChunksOfExpectedSizes() {
        String text = generateSequentialText(1000);

        List<String> chunks = chunkingService.chunk(text);

        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(500);
        assertThat(chunks.get(2)).hasSize(100);
        assertThat(chunks.get(0).substring(450)).isEqualTo(chunks.get(1).substring(0, 50));
        assertThat(chunks.get(1).substring(450)).isEqualTo(chunks.get(2).substring(0, 50));
        assertThat(chunks.get(0)).isEqualTo(text.substring(0, 500));
        assertThat(chunks.get(1)).isEqualTo(text.substring(450, 950));
        assertThat(chunks.get(2)).isEqualTo(text.substring(900));
    }

    private String generateSequentialText(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char next = (char) ('a' + (i % 26));
            builder.append(next);
        }
        return builder.toString();
    }
}
