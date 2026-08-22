package com.flowset.vendor.dto;

import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record SocialLinksRequest(
        @URL(message = "Instagram must be a valid URL")
        @Size(max = 500)
        String instagram,

        @URL(message = "Facebook must be a valid URL")
        @Size(max = 500)
        String facebook,

        @URL(message = "LinkedIn must be a valid URL")
        @Size(max = 500)
        String linkedin,

        @URL(message = "YouTube must be a valid URL")
        @Size(max = 500)
        String youtube,

        @URL(message = "Pinterest must be a valid URL")
        @Size(max = 500)
        String pinterest,

        @URL(message = "Website must be a valid URL")
        @Size(max = 500)
        String website
) {
}
