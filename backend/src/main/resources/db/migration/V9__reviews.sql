-- Vendor-level reviews and ratings. One review per completed order.
-- Soft-deleted rows keep UNIQUE(order_id) so a planner cannot submit a second review.
-- Per-review stars are SMALLINT 1–5 (no floating point). Vendor aggregate cache is vendors.rating NUMERIC(3,2).

CREATE TABLE reviews (
    id                          UUID PRIMARY KEY,
    order_id                    UUID         NOT NULL,
    vendor_id                   UUID         NOT NULL,
    planner_id                  UUID         NOT NULL,
    overall_rating              SMALLINT     NOT NULL,
    communication_rating        SMALLINT,
    product_quality_rating      SMALLINT,
    delivery_rating             SMALLINT,
    value_rating                SMALLINT,
    review_text                 TEXT,
    status                      VARCHAR(20)  NOT NULL,
    is_verified                 BOOLEAN      NOT NULL DEFAULT TRUE,
    vendor_response_text        TEXT,
    vendor_response_at          TIMESTAMP WITH TIME ZONE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    published_at                TIMESTAMP WITH TIME ZONE,
    deleted_at                  TIMESTAMP WITH TIME ZONE,
    deleted_by                  UUID,
    delete_reason               VARCHAR(500),
    CONSTRAINT uq_reviews_order UNIQUE (order_id),
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_reviews_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT fk_reviews_planner FOREIGN KEY (planner_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_deleted_by FOREIGN KEY (deleted_by) REFERENCES users (id),
    CONSTRAINT ck_reviews_status CHECK (status IN (
        'PENDING', 'PUBLISHED', 'HIDDEN', 'REMOVED', 'REPORTED'
    )),
    CONSTRAINT ck_reviews_overall CHECK (overall_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_communication CHECK (communication_rating IS NULL OR communication_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_quality CHECK (product_quality_rating IS NULL OR product_quality_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_delivery CHECK (delivery_rating IS NULL OR delivery_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_value CHECK (value_rating IS NULL OR value_rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_vendor_id ON reviews (vendor_id);
CREATE INDEX idx_reviews_planner_id ON reviews (planner_id);
CREATE INDEX idx_reviews_order_id ON reviews (order_id);
CREATE INDEX idx_reviews_status ON reviews (status);
CREATE INDEX idx_reviews_created_at ON reviews (created_at);
CREATE INDEX idx_reviews_overall_rating ON reviews (overall_rating);
CREATE INDEX idx_reviews_vendor_public ON reviews (vendor_id, status, created_at DESC);
CREATE INDEX idx_reviews_published_at ON reviews (published_at);

CREATE TABLE review_reports (
    id              UUID PRIMARY KEY,
    review_id       UUID         NOT NULL,
    reported_by     UUID         NOT NULL,
    reason          VARCHAR(40)  NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_review_reports_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    CONSTRAINT fk_review_reports_reporter FOREIGN KEY (reported_by) REFERENCES users (id),
    CONSTRAINT ck_review_reports_reason CHECK (reason IN (
        'SPAM', 'ABUSIVE', 'FAKE', 'INAPPROPRIATE', 'PERSONAL_INFORMATION', 'OTHER'
    )),
    CONSTRAINT ck_review_reports_status CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_review_reports_review_id ON review_reports (review_id);
CREATE INDEX idx_review_reports_reported_by ON review_reports (reported_by);
CREATE INDEX idx_review_reports_status ON review_reports (status);
CREATE INDEX idx_review_reports_created_at ON review_reports (created_at);

CREATE TABLE review_moderation_actions (
    id              UUID PRIMARY KEY,
    review_id       UUID         NOT NULL,
    actor_id        UUID         NOT NULL,
    action          VARCHAR(40)  NOT NULL,
    note            TEXT,
    previous_status VARCHAR(20),
    new_status      VARCHAR(20),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_review_moderation_review FOREIGN KEY (review_id) REFERENCES reviews (id),
    CONSTRAINT fk_review_moderation_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT ck_review_moderation_action CHECK (action IN (
        'HIDE', 'RESTORE', 'REMOVE', 'NOTE'
    ))
);

CREATE INDEX idx_review_moderation_review_id ON review_moderation_actions (review_id);
CREATE INDEX idx_review_moderation_created_at ON review_moderation_actions (created_at);

INSERT INTO notification_templates (id, type, title_template, body_template, created_at) VALUES
    ('a1000000-0000-0000-0000-000000000018', 'REVIEW_ELIGIBLE',
        'How was your experience?',
        'Leave a review for {vendorName} after order {orderNumber}.',
        NOW()),
    ('a1000000-0000-0000-0000-000000000019', 'REVIEW_RECEIVED',
        '{title}',
        '{message}',
        NOW()),
    ('a1000000-0000-0000-0000-00000000001a', 'REVIEW_REPORTED',
        'Review reported',
        'A review was reported ({reason}).',
        NOW()),
    ('a1000000-0000-0000-0000-00000000001b', 'REVIEW_MODERATED',
        'Review update',
        'A review for {vendorName} was {action}.',
        NOW());
