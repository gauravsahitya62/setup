package com.flowset.notifications.repository;

import com.flowset.common.NotificationFilter;
import com.flowset.common.NotificationType;
import com.flowset.notifications.entity.NotificationEntity;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecifications {

    private NotificationSpecifications() {
    }

    public static Specification<NotificationEntity> inbox(UUID recipientUserId, NotificationFilter filter) {
        Specification<NotificationEntity> spec = (root, query, cb) ->
                cb.equal(root.get("recipientUserId"), recipientUserId);
        if (filter == NotificationFilter.UNREAD) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("read")));
        } else if (filter != null && filter != NotificationFilter.ALL) {
            Set<NotificationType> types = NotificationType.typesFor(filter);
            spec = spec.and((root, query, cb) -> root.get("type").in(types));
        }
        return spec;
    }
}
