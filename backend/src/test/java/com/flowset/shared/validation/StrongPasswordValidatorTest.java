package com.flowset.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void acceptsStrongPassword() {
        assertThat(validator.isValid("Str0ng!Pass", null)).isTrue();
    }

    @Test
    void rejectsWeakPassword() {
        assertThat(validator.isValid("password", null)).isFalse();
        assertThat(validator.isValid("Password1", null)).isFalse();
        assertThat(validator.isValid("short1!", null)).isFalse();
    }
}
