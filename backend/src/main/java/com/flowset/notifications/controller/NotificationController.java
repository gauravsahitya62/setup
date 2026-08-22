package com.flowset.notifications.controller;

import com.flowset.common.NotificationFilter;
import com.flowset.notifications.dto.NotificationResponse;
import com.flowset.notifications.dto.UnreadCountResponse;
import com.flowset.notifications.service.NotificationService;
import com.flowset.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(
        name = "Notifications",
        description = """
                In-app notifications for the authenticated user. Cross-user access returns 404.
                Filter: ALL, UNREAD, BOOKINGS, ORDERS, DELIVERY, INVENTORY, SYSTEM. Newest first.
                """
)
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Page of notifications",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "content": [{
                                "id": "11111111-1111-1111-1111-111111111111",
                                "type": "BOOKING_REQUEST_ACCEPTED",
                                "title": "Booking accepted",
                                "message": "Royal Event Rentals accepted your booking request for Sharma Wedding.",
                                "resourceType": "BOOKING_REQUEST",
                                "resourceId": "22222222-2222-2222-2222-222222222222",
                                "actionUrl": "/planner/booking-requests/22222222-2222-2222-2222-222222222222",
                                "isRead": false,
                                "createdAt": "2026-08-22T09:00:00Z"
                              }],
                              "page": 0,
                              "size": 20,
                              "totalElements": 1,
                              "totalPages": 1
                            }
                            """))),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public PageResponse<NotificationResponse> list(
            @RequestParam(required = false, defaultValue = "ALL") NotificationFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return notificationService.getNotifications(filter, page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread notification count")
    @ApiResponse(
            responseCode = "200",
            content = @Content(examples = @ExampleObject(value = """
                    { "unreadCount": 3 }
                    """)))
    public UnreadCountResponse unreadCount() {
        return notificationService.getUnreadCount();
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not found (also used to hide other users' notifications)",
                    content = @Content(examples = @ExampleObject(value = """
                            { "status": 404, "error": "Not Found", "message": "Notification not found" }
                            """)))
    })
    public NotificationResponse markRead(@PathVariable UUID id) {
        return notificationService.markAsRead(id);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public UnreadCountResponse markAllRead() {
        return notificationService.markAllAsRead();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a notification")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public void delete(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
    }
}
