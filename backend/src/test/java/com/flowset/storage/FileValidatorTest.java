package com.flowset.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flowset.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileValidatorTest {

    @Test
    void acceptsPngMagicBytes() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        assertThat(FileValidator.validate(file, AllowedContentTypes.IMAGES)).isEqualTo("image/png");
    }

    @Test
    void rejectsDeclaredPngWithNonPngContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", "not-a-png".getBytes());
        assertThatThrownBy(() -> FileValidator.validate(file, AllowedContentTypes.IMAGES))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsExecutableExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payload.exe",
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        assertThatThrownBy(() -> FileValidator.validate(file, AllowedContentTypes.IMAGES))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPathTraversalName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../secret.png",
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
        assertThatThrownBy(() -> FileValidator.validate(file, AllowedContentTypes.IMAGES))
                .isInstanceOf(BadRequestException.class);
    }
}
