package com.flowset.common;

/**
 * Catalog of planner event types. Stored as VARCHAR ({@code EnumType.STRING}).
 * Adding a type is a code change (enum + frontend labels) — no Flyway CHECK —
 * so new values can ship without a schema migration.
 */
public enum EventType {
    WEDDING,
    RECEPTION,
    ENGAGEMENT,
    HALDI,
    MEHENDI,
    CORPORATE,
    CONFERENCE,
    EXHIBITION,
    CONCERT,
    FILM_SHOOT,
    PRIVATE_EVENT,
    OTHER
}
