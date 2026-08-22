package com.flowset.storage;

import java.util.Set;

public final class AllowedContentTypes {

    public static final Set<String> IMAGES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    public static final Set<String> IMAGES_AND_PDF = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf"
    );

    public static final Set<String> PROJECT_DOCUMENTS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "text/plain"
    );

    private AllowedContentTypes() {
    }
}
