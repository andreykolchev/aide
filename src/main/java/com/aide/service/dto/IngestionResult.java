package com.aide.service.dto;

import java.nio.file.Path;

public record IngestionResult(String text, Path storedPath) {
}
