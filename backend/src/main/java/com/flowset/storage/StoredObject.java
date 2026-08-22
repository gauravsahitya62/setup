package com.flowset.storage;

public record StoredObject(
        String relativePath,
        String publicUrl,
        String originalFilename,
        String contentType,
        long sizeBytes
) {
}
