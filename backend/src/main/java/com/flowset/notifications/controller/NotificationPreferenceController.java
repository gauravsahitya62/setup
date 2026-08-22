package com.flowset.notifications.controller;

import com.flowset.notifications.dto.NotificationPreferencesRequest;
import com.flowset.notifications.dto.NotificationPreferencesResponse;
import com.flowset.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-preferences")
@Tag(name = "Notification Preferences", description = "Per-user in-app notification preferences. Defaults are enabled.")
@SecurityRequirement(name = "bearerAuth")
public class NotificationPreferenceController {

    private final NotificationService notificationService;

    public NotificationPreferenceController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get notification preferences")
    @ApiResponse(
            responseCode = "200",
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "bookingUpdates": true,
                      "orderUpdates": true,
                      "deliveryUpdates": true,
                      "inventoryAlerts": true,
                      "systemAnnouncements": true
                    }
                    """)))
    public NotificationPreferencesResponse get() {
        return notificationService.getPreferences();
    }

    @PutMapping
    @Operation(summary = "Replace notification preferences")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid preference",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 400, "error": "Validation Failed", "message": "bookingUpdates is required" }
                            """)))
    })
    public NotificationPreferencesResponse update(@Valid @RequestBody NotificationPreferencesRequest request) {
        return notificationService.updatePreferences(request);
    }
}
