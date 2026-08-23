package com.flowset.storage;

import com.flowset.config.StorageProperties;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.NotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/**
 * Private-by-default S3 storage. Objects are not made publicly readable. Access is gated by
 * {@link FileController} (authenticated streaming). IAM task roles are preferred over static keys.
 */
@Service
@ConditionalOnProperty(name = "flowset.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3;
    private final String bucket;
    private final String publicBaseUrl;

    public S3StorageService(StorageProperties properties) {
        if (!StringUtils.hasText(properties.s3Bucket()) || !StringUtils.hasText(properties.s3Region())) {
            throw new IllegalStateException("AWS_S3_BUCKET and AWS_REGION are required when STORAGE_PROVIDER=s3");
        }
        this.bucket = properties.s3Bucket();
        this.publicBaseUrl = properties.publicBaseUrl().replaceAll("/$", "");
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.s3Region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (StringUtils.hasText(properties.s3Endpoint())) {
            builder.endpointOverride(URI.create(properties.s3Endpoint())).forcePathStyle(true);
        }
        this.s3 = builder.build();
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
        if (safeFolder.contains("..")) {
            throw new BadRequestException("Invalid storage folder");
        }
        String key = safeFolder + "/" + UUID.randomUUID() + extension;
        try {
            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .serverSideEncryption(ServerSideEncryption.AES256)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("Stored s3 object key={} size={}", key, file.getSize());
            return new StoredObject(key, toPublicUrl(key), original, contentType, file.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    @Override
    public Resource loadAsResource(String relativePath) {
        String key = normalizeKey(relativePath);
        try {
            ResponseBytes<GetObjectResponse> bytes = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return new ByteArrayResource(bytes.asByteArray());
        } catch (NoSuchKeyException e) {
            throw new NotFoundException("File not found");
        } catch (RuntimeException e) {
            log.warn("Failed to load s3 object");
            throw new NotFoundException("File not found");
        }
    }

    @Override
    public void delete(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return;
        }
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(normalizeKey(relativePath)).build());
            log.info("Deleted s3 object key={}", normalizeKey(relativePath));
        } catch (RuntimeException e) {
            log.warn("Failed to delete s3 object");
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

    private static String normalizeKey(String relativePath) {
        String key = relativePath.replace("\\", "/").replaceAll("^/+", "");
        if (key.contains("..")) {
            throw new BadRequestException("Invalid file path");
        }
        return key;
    }
}
