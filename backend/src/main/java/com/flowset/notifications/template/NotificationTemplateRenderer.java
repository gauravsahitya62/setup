package com.flowset.notifications.template;

import com.flowset.common.NotificationType;
import com.flowset.notifications.entity.NotificationTemplateEntity;
import com.flowset.notifications.repository.NotificationTemplateRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NotificationTemplateRenderer {

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplateRenderer(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public RenderedTemplate render(NotificationType type, Map<String, Object> metadata) {
        NotificationTemplateEntity template = templateRepository.findByType(type).orElse(null);
        String title = template != null ? template.getTitleTemplate() : defaultTitle(type);
        String body = template != null ? template.getBodyTemplate() : defaultBody(type);
        return new RenderedTemplate(apply(title, metadata), apply(body, metadata));
    }

    private String apply(String template, Map<String, Object> metadata) {
        if (!StringUtils.hasText(template)) {
            return "";
        }
        String result = template;
        if (metadata != null) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                result = result.replace("{" + entry.getKey() + "}", value);
            }
        }
        return result.replaceAll("\\{[a-zA-Z0-9]+}", "").trim();
    }

    private static String defaultTitle(NotificationType type) {
        return type.name().replace('_', ' ').toLowerCase();
    }

    private static String defaultBody(NotificationType type) {
        return defaultTitle(type);
    }

    public record RenderedTemplate(String title, String message) {
    }
}
