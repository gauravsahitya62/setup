# Notifications

In-app notification center (no chat, WhatsApp, or WebSockets in MVP).

Review events (`REVIEW_ELIGIBLE`, `REVIEW_RECEIVED`, `REVIEW_REPORTED`, `REVIEW_MODERATED`) are documented in [events](./notification-events.md). Vendor notifications never include review body text.

- [Architecture](./notification-architecture.md)
- [Events](./notification-events.md)
- [Business rules](./notification-business-rules.md)
- [ADR: after-commit delivery](../architecture/decisions/0003-notification-after-commit.md)
