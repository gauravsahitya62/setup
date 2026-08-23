package com.flowset.storage;

import com.flowset.exceptions.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploads by declared type, extension, size, and magic bytes. Does not trust the client MIME type alone.
 */
public final class FileValidator {

    private static final long DEFAULT_MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".com", ".js", ".mjs", ".sh", ".php", ".jsp", ".html", ".htm", ".svg", ".xml");

    private FileValidator() {
    }

    public static String validate(MultipartFile file, Set<String> allowedContentTypes, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("File exceeds the maximum allowed size");
        }

        String original = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String cleaned = original.replace("\\", "/");
        if (cleaned.contains("..") || cleaned.startsWith("/")) {
            throw new BadRequestException("Invalid file name");
        }
        String extension = extensionOf(cleaned);
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("This file type is not allowed");
        }

        String declared = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        Set<String> allowed = allowedContentTypes == null || allowedContentTypes.isEmpty()
                ? AllowedContentTypes.IMAGES_AND_PDF
                : allowedContentTypes;
        if (!allowed.contains(declared)) {
            throw new BadRequestException("Unsupported file type");
        }

        String detected = detectContentType(file, extension);
        if (!allowed.contains(detected)) {
            throw new BadRequestException("File content does not match an allowed type");
        }
        if (!declared.equals(detected) && !(isJpeg(declared) && isJpeg(detected))) {
            throw new BadRequestException("Declared file type does not match file content");
        }
        return detected;
    }

    public static String validate(MultipartFile file, Set<String> allowedContentTypes) {
        return validate(file, allowedContentTypes, DEFAULT_MAX_BYTES);
    }

    public static String detectContentType(MultipartFile file, String extension) {
        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(header);
        } catch (IOException e) {
            throw new BadRequestException("Unable to read file");
        }
        if (read < 4) {
            throw new BadRequestException("File content is invalid");
        }
        if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
            return "image/png";
        }
        if (header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38) {
            return "image/gif";
        }
        if (read >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) {
            return "image/webp";
        }
        if (header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46) {
            return "application/pdf";
        }
        if (header[0] == 0x50 && header[1] == 0x4B) {
            if (".docx".equals(extension)) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            if (".xlsx".equals(extension)) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
        }
        if (".csv".equals(extension)) {
            return "text/csv";
        }
        if (".txt".equals(extension)) {
            return "text/plain";
        }
        if (".doc".equals(extension)) {
            return "application/msword";
        }
        if (".xls".equals(extension)) {
            return "application/vnd.ms-excel";
        }
        throw new BadRequestException("Unrecognized file content");
    }

    private static boolean isJpeg(String type) {
        return "image/jpeg".equals(type) || "image/jpg".equals(type);
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }
}
