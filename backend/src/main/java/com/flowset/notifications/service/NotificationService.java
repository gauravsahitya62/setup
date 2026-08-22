package com.flowset.notifications.service;

import com.flowset.common.NotificationFilter;
import com.flowset.common.NotificationPreferenceGroup;
import com.flowset.common.RoleName;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.notifications.channel.NotificationChannel;
import com.flowset.notifications.dto.CreateNotificationCommand;
import com.flowset.notifications.dto.NotificationPreferencesRequest;
import com.flowset.notifications.dto.NotificationPreferencesResponse;
import com.flowset.notifications.dto.NotificationResponse;
import com.flowset.notifications.dto.UnreadCountResponse;
import com.flowset.notifications.entity.NotificationEntity;
import com.flowset.notifications.entity.NotificationPreferenceEntity;
import com.flowset.notifications.exception.NotificationNotFoundException;
import com.flowset.notifications.mapper.NotificationMapper;
import com.flowset.notifications.repository.NotificationPreferenceRepository;
import com.flowset.notifications.repository.NotificationRepository;
import com.flowset.notifications.repository.NotificationSpecifications;
import com.flowset.notifications.template.NotificationTemplateRenderer;
import com.flowset.notifications.template.NotificationTemplateRenderer.RenderedTemplate;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.dto.PageResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationMapper mapper;
    private final NotificationTemplateRenderer templateRenderer;
    private final List<NotificationChannel> channels;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            NotificationMapper mapper,
            NotificationTemplateRenderer templateRenderer,
            List<NotificationChannel> channels
    ) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.mapper = mapper;
        this.templateRenderer = templateRenderer;
        this.channels = channels;
    }

    /**
     * Persists an in-app notification. Skips when the recipient disabled the
     * preference group or an unread duplicate already exists for the same
     * recipient + type + resource. Delivery-channel failures are logged and
     * never thrown to the caller.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse createNotification(CreateNotificationCommand command) {
        if (command == null || command.recipientUserId() == null || command.type() == null) {
            throw new BadRequestException("Invalid notification type");
        }
        if (!isEnabled(command.recipientUserId(), command.type().preferenceGroup())) {
            log.debug("Skipping notification type={} recipient={} preference disabled",
                    command.type(), command.recipientUserId());
            return null;
        }
        if (command.resourceId() != null
                && notificationRepository.existsByRecipientUserIdAndTypeAndResourceIdAndReadFalse(
                        command.recipientUserId(), command.type(), command.resourceId())) {
            log.debug("Skipping duplicate unread notification type={} resource={}",
                    command.type(), command.resourceId());
            return null;
        }

        RenderedTemplate rendered = templateRenderer.render(command.type(), command.metadata());
        NotificationEntity entity = new NotificationEntity();
        entity.setRecipientUserId(command.recipientUserId());
        entity.setType(command.type());
        entity.setTitle(trimToLength(rendered.title(), 255));
        entity.setMessage(rendered.message());
        entity.setResourceType(command.resourceType());
        entity.setResourceId(command.resourceId());
        entity.setActionUrl(command.actionUrl());
        entity.setMetadata(mapper.serializeMetadata(command.metadata()));
        entity.setRead(false);
        NotificationEntity saved = notificationRepository.save(entity);
        deliver(saved);
        return mapper.toResponse(saved);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<NotificationResponse> createBulkNotifications(List<CreateNotificationCommand> commands) {
        List<NotificationResponse> created = new ArrayList<>();
        if (commands == null) {
            return created;
        }
        for (CreateNotificationCommand command : commands) {
            try {
                NotificationResponse response = createNotification(command);
                if (response != null) {
                    created.add(response);
                }
            } catch (RuntimeException ex) {
                log.error("Failed to create notification type={} recipient={}",
                        command == null ? null : command.type(),
                        command == null ? null : command.recipientUserId(),
                        ex);
            }
        }
        return created;
    }

    @Transactional
    public NotificationResponse markAsRead(UUID id) {
        UUID userId = requireUserId();
        NotificationEntity entity = notificationRepository
                .findByIdAndRecipientUserId(id, userId)
                .orElseThrow(NotificationNotFoundException::new);
        if (!entity.isRead()) {
            entity.setRead(true);
            entity.setReadAt(Instant.now());
            notificationRepository.save(entity);
        }
        return mapper.toResponse(entity);
    }

    @Transactional
    public UnreadCountResponse markAllAsRead() {
        UUID userId = requireUserId();
        notificationRepository.markAllRead(userId, Instant.now());
        return new UnreadCountResponse(0);
    }

    @Transactional
    public void deleteNotification(UUID id) {
        UUID userId = requireUserId();
        NotificationEntity entity = notificationRepository
                .findByIdAndRecipientUserId(id, userId)
                .orElseThrow(NotificationNotFoundException::new);
        notificationRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(notificationRepository.countByRecipientUserIdAndReadFalse(requireUserId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getNotifications(NotificationFilter filter, int page, int size) {
        UUID userId = requireUserId();
        NotificationFilter resolved = filter == null ? NotificationFilter.ALL : filter;
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<NotificationEntity> result = notificationRepository.findAll(
                NotificationSpecifications.inbox(userId, resolved),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public NotificationPreferencesResponse getPreferences() {
        return mapper.toPreferences(requirePreferences(requireUserId()));
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(NotificationPreferencesRequest request) {
        if (request == null
                || request.bookingUpdates() == null
                || request.orderUpdates() == null
                || request.deliveryUpdates() == null
                || request.inventoryAlerts() == null
                || request.systemAnnouncements() == null) {
            throw new BadRequestException("Invalid preference");
        }
        NotificationPreferenceEntity entity = requirePreferences(requireUserId());
        entity.setBookingUpdates(request.bookingUpdates());
        entity.setOrderUpdates(request.orderUpdates());
        entity.setDeliveryUpdates(request.deliveryUpdates());
        entity.setInventoryAlerts(request.inventoryAlerts());
        entity.setSystemAnnouncements(request.systemAnnouncements());
        return mapper.toPreferences(preferenceRepository.save(entity));
    }

    boolean isEnabled(UUID userId, NotificationPreferenceGroup group) {
        NotificationPreferenceEntity prefs = preferenceRepository.findByUserId(userId).orElse(null);
        if (prefs == null) {
            return true;
        }
        return switch (group) {
            case BOOKING_UPDATES -> prefs.isBookingUpdates();
            case ORDER_UPDATES -> prefs.isOrderUpdates();
            case DELIVERY_UPDATES -> prefs.isDeliveryUpdates();
            case INVENTORY_ALERTS -> prefs.isInventoryAlerts();
            case SYSTEM_ANNOUNCEMENTS -> prefs.isSystemAnnouncements();
        };
    }

    private NotificationPreferenceEntity requirePreferences(UUID userId) {
        return preferenceRepository.findByUserId(userId).orElseGet(() -> {
            NotificationPreferenceEntity created = new NotificationPreferenceEntity();
            created.setUserId(userId);
            created.setBookingUpdates(true);
            created.setOrderUpdates(true);
            created.setDeliveryUpdates(true);
            created.setInventoryAlerts(true);
            created.setSystemAnnouncements(true);
            return preferenceRepository.save(created);
        });
    }

    private void deliver(NotificationEntity notification) {
        for (NotificationChannel channel : channels) {
            try {
                channel.deliver(notification);
            } catch (RuntimeException ex) {
                log.error("Notification channel {} failed for id={}", channel.type(), notification.getId(), ex);
            }
        }
    }

    private UUID requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        if (principal.getRole() != RoleName.PLANNER
                && principal.getRole() != RoleName.VENDOR
                && principal.getRole() != RoleName.ADMIN) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal.getId();
    }

    private static String trimToLength(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}
