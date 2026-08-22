package com.flowset.storage;

import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    StoredObject store(MultipartFile file, String folder);

    StoredObject store(MultipartFile file, String folder, Set<String> allowedContentTypes);

    Resource loadAsResource(String relativePath);

    void delete(String relativePath);

    String toPublicUrl(String relativePath);
}
