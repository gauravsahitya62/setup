package com.flowset.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugUtilsTest {

    @Test
    void slugifiesCompanyNames() {
        assertThat(SlugUtils.slugify("Atlas Event Rentals")).isEqualTo("atlas-event-rentals");
        assertThat(SlugUtils.slugify("  Harbor & Grove  ")).isEqualTo("harbor-grove");
    }

    @Test
    void uniqueSlugAppendsSuffix() {
        String slug = SlugUtils.uniqueSlug("Atlas", s -> s.equals("atlas") || s.equals("atlas-2"));
        assertThat(slug).isEqualTo("atlas-3");
    }
}
