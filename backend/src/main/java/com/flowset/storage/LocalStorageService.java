package com.flowset.storage;

import com.flowset.config.StorageProperties;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.NotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "flowset.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path root;
    private final String publicBaseUrl;

    public LocalStorageService(StorageProperties properties) {
        this.root = Paths.get(properties.localRoot()).toAbsolutePath().normalize();
        this.publicBaseUrl = properties.publicBaseUrl().replaceAll("/$", "");
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create storage root: " + this.root, e);
        }
    }

    @Override
    public StoredObject store(MultipartFile file, String folder) {
        return store(file, folder, AllowedContentTypes.IMAGES_AND_PDF);
    }

    @Override
    public StoredObject store(MultipartFile file, String folder, Set<String> allowedContentTypes) {
        String contentType = FileValidator.validate(file, allowedContentTypes);
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot).toLowerCase(Locale.ROOT);
        }

        String safeFolder = folder == null || folder.isBlank() ? "misc" : folder.replaceAll("[^a-zA-Z0-9/_-]", "");
        String filename = UUID.randomUUID() + extension;
        Path destinationDir = root.resolve(safeFolder).normalize();
        if (!destinationDir.startsWith(root)) {
            throw new BadRequestException("Invalid storage folder");
        }

        try {
            Files.createDirectories(destinationDir);
            Path target = destinationDir.resolve(filename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String relative = safeFolder + "/" + filename;
            log.info("Stored file relativePath={} size={}", relative, file.getSize());
            return new StoredObject(relative, toPublicUrl(relative), original, contentType, file.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) {
        try {
            Path file = resolve(relativePath);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("File not found");
            }
            return resource;
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new NotFoundException("File not found");
        }
    }

    @Override
    public void delete(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        try {
            Path file = resolve(relativePath);
            Files.deleteIfExists(file);
            log.info("Deleted file relativePath={}", relativePath);
        } catch (IOException e) {
            log.warn("Failed to delete file relativePath={}", relativePath);
        }
    }

    @Override
    public String toPublicUrl(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        return publicBaseUrl + "/" + relativePath.replace("\\", "/");
    }

    private Path resolve(String relativePath) {
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root)) {
            throw new BadRequestException("Invalid file path");
        }
        return file;
    }
}
