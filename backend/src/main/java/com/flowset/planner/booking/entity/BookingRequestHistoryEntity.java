package com.flowset.planner.booking.entity;

import com.flowset.common.BookingRequestStatus;
import com.flowset.common.BookingTimelineEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_request_status_history")
public class BookingRequestHistoryEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_request_id", nullable = false)
    private BookingRequestEntity bookingRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private BookingRequestStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private BookingRequestStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private BookingTimelineEventType eventType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public BookingRequestEntity getBookingRequest() {
        return bookingRequest;
    }

    public void setBookingRequest(BookingRequestEntity bookingRequest) {
        this.bookingRequest = bookingRequest;
    }

    public BookingRequestStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(BookingRequestStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public BookingRequestStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(BookingRequestStatus toStatus) {
        this.toStatus = toStatus;
    }

    public BookingTimelineEventType getEventType() {
        return eventType;
    }

    public void setEventType(BookingTimelineEventType eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
