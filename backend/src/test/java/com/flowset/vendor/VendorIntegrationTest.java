package com.flowset.vendor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VendorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationCreatesEmptyDraftStorefront() throws Exception {
        String email = "vendor.auto@" + UUID.randomUUID() + ".example.com";
        String accessToken = registerVendor(email, "Royal Event Rentals");

        mockMvc.perform(get("/api/v1/vendors/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Royal Event Rentals"))
                .andExpect(jsonPath("$.slug").value("royal-event-rentals"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.serviceAreas").isEmpty())
                .andExpect(jsonPath("$.portfolio").isEmpty());
    }

    @Test
    void vendorCanCompleteAndPublishPublicSlug() throws Exception {
        String email = "vendor.store@" + UUID.randomUUID() + ".example.com";
        String accessToken = registerVendor(email, "Atlas Event Rentals");

        MvcResult me = mockMvc.perform(get("/api/v1/vendors/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode vendor = objectMapper.readTree(me.getResponse().getContentAsString());
        String vendorId = vendor.path("id").asText();
        String slug = vendor.path("slug").asText();
        assertThat(slug).isEqualTo("atlas-event-rentals");

        Map<String, Object> update = new HashMap<>();
        update.put("companyName", "Atlas Event Rentals");
        update.put("shortDescription", "Seating and tables");
        update.put("businessType", "Rentals");
        update.put("establishedYear", 2012);
        update.put("email", email);
        update.put("phone", "+1 555 0100");
        update.put("status", "PUBLISHED");

        mockMvc.perform(put("/api/v1/vendors/{id}", vendorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/v1/vendors/{id}/service-areas", vendorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "country", "USA",
                                "state", "CA",
                                "city", "Los Angeles"))))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/vendors/{id}/social-links", vendorId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "instagram", "https://instagram.com/atlas",
                                "website", "https://atlas.example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instagram").value("https://instagram.com/atlas"));

        MockMultipartFile logo = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                "fake-png-bytes".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/vendors/{id}/logo", vendorId)
                        .file(logo)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").isNotEmpty());

        mockMvc.perform(get("/api/v1/vendors/slug/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Atlas Event Rentals"))
                .andExpect(jsonPath("$.serviceAreas[0].city").value("Los Angeles"));

        mockMvc.perform(get("/api/v1/vendors")
                        .param("city", "Los Angeles")
                        .param("verified", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].slug").value(slug));
    }

    @Test
    void draftVendorIsHiddenFromPublic() throws Exception {
        String email = "draft.vendor@" + UUID.randomUUID() + ".example.com";
        String accessToken = registerVendor(email, "Hidden Draft Co");

        MvcResult me = mockMvc.perform(get("/api/v1/vendors/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        String slug = objectMapper.readTree(me.getResponse().getContentAsString()).path("slug").asText();

        mockMvc.perform(get("/api/v1/vendors/slug/{slug}", slug)).andExpect(status().isNotFound());
    }

    @Test
    void plannerCannotCreateOrEditVendorStorefront() throws Exception {
        String email = "planner.x@" + UUID.randomUUID() + ".example.com";
        MvcResult register = mockMvc.perform(post("/api/v1/auth/register/planner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Maya",
                                "lastName", "Chen",
                                "email", email,
                                "password", "Str0ng!Pass"))))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(register.getResponse().getContentAsString())
                .path("accessToken")
                .asText();

        mockMvc.perform(post("/api/v1/vendors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyName", "Nope"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/vendors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationRejectsBlankCompanyName() throws Exception {
        String token = registerVendor("validate@" + UUID.randomUUID() + ".example.com", "Validate Co");
        MvcResult me = mockMvc.perform(get("/api/v1/vendors/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String id = objectMapper.readTree(me.getResponse().getContentAsString()).path("id").asText();

        mockMvc.perform(put("/api/v1/vendors/{id}", id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("companyName", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    private String registerVendor(String email, String businessName) throws Exception {
        MvcResult register = mockMvc.perform(post("/api/v1/auth/register/vendor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "firstName", "Alex",
                                "lastName", "Rivera",
                                "email", email,
                                "password", "Str0ng!Pass",
                                "businessName", businessName))))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(register.getResponse().getContentAsString())
                .path("accessToken")
                .asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
