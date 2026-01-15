package com.aide.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChunkingService {

    private final int chunkSize;
    private final int overlap;

    @Autowired
    public ChunkingService(
            @Value("${app.chunking.size:500}") int chunkSize,
            @Value("${app.chunking.overlap:50}") int overlap
    ) {
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = text.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(text.substring(start, end));

            if (end == length) {
                break;
            }

            start = end - overlap;
        }

        return chunks;
    }
}
