-- Inventory availability engine: reservations, blocks, movements, maintenance, optimistic locking

ALTER TABLE inventory
    ADD COLUMN created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

ALTER TABLE inventory
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

CREATE TABLE inventory_reservations (
    id                   UUID PRIMARY KEY,
    product_variant_id   UUID            NOT NULL,
    booking_request_id   UUID,
    quantity             INTEGER         NOT NULL,
    start_date           DATE            NOT NULL,
    end_date             DATE            NOT NULL,
    status               VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventory_reservations_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_inventory_reservations_qty CHECK (quantity > 0),
    CONSTRAINT ck_inventory_reservations_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_inventory_reservations_status CHECK (
        status IN ('PENDING', 'CONFIRMED', 'RELEASED', 'CANCELLED', 'EXPIRED')
    )
);

CREATE INDEX idx_inventory_reservations_variant ON inventory_reservations (product_variant_id);
CREATE INDEX idx_inventory_reservations_start ON inventory_reservations (start_date);
CREATE INDEX idx_inventory_reservations_end ON inventory_reservations (end_date);
CREATE INDEX idx_inventory_reservations_status ON inventory_reservations (status);
CREATE INDEX idx_inventory_reservations_booking ON inventory_reservations (booking_request_id);
CREATE INDEX idx_inventory_reservations_overlap ON inventory_reservations (product_variant_id, status, start_date, end_date);

CREATE TABLE inventory_blocks (
    id                   UUID PRIMARY KEY,
    product_variant_id   UUID            NOT NULL,
    quantity             INTEGER         NOT NULL,
    start_date           DATE            NOT NULL,
    end_date             DATE            NOT NULL,
    reason               VARCHAR(40)     NOT NULL,
    notes                VARCHAR(500),
    status               VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventory_blocks_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_inventory_blocks_qty CHECK (quantity > 0),
    CONSTRAINT ck_inventory_blocks_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_inventory_blocks_reason CHECK (
        reason IN ('MAINTENANCE', 'PRIVATE_BOOKING', 'DAMAGED', 'VENDOR_UNAVAILABLE', 'OTHER')
    ),
    CONSTRAINT ck_inventory_blocks_status CHECK (status IN ('ACTIVE', 'RELEASED'))
);

CREATE INDEX idx_inventory_blocks_variant ON inventory_blocks (product_variant_id);
CREATE INDEX idx_inventory_blocks_start ON inventory_blocks (start_date);
CREATE INDEX idx_inventory_blocks_end ON inventory_blocks (end_date);
CREATE INDEX idx_inventory_blocks_status ON inventory_blocks (status);
CREATE INDEX idx_inventory_blocks_overlap ON inventory_blocks (product_variant_id, status, start_date, end_date);

CREATE TABLE inventory_movements (
    id                   UUID PRIMARY KEY,
    product_variant_id   UUID            NOT NULL,
    quantity             INTEGER         NOT NULL,
    movement_type        VARCHAR(30)     NOT NULL,
    reason               VARCHAR(500)    NOT NULL,
    reference_id         UUID,
    created_by           UUID,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventory_movements_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT ck_inventory_movements_type CHECK (
        movement_type IN (
            'STOCK_ADDED', 'STOCK_REMOVED', 'DAMAGED', 'REPAIRED',
            'MAINTENANCE', 'ADJUSTMENT', 'RETURNED'
        )
    )
);

CREATE INDEX idx_inventory_movements_variant ON inventory_movements (product_variant_id);
CREATE INDEX idx_inventory_movements_created ON inventory_movements (created_at);
CREATE INDEX idx_inventory_movements_type ON inventory_movements (movement_type);

CREATE TABLE maintenance_records (
    id                   UUID PRIMARY KEY,
    product_variant_id   UUID            NOT NULL,
    block_id             UUID            NOT NULL,
    quantity             INTEGER         NOT NULL,
    start_date           DATE            NOT NULL,
    end_date             DATE            NOT NULL,
    reason               VARCHAR(500)    NOT NULL,
    status               VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_maintenance_records_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_maintenance_records_block FOREIGN KEY (block_id) REFERENCES inventory_blocks (id),
    CONSTRAINT ck_maintenance_records_qty CHECK (quantity > 0),
    CONSTRAINT ck_maintenance_records_dates CHECK (end_date >= start_date),
    CONSTRAINT ck_maintenance_records_status CHECK (status IN ('ACTIVE', 'REMOVED'))
);

CREATE INDEX idx_maintenance_records_variant ON maintenance_records (product_variant_id);
CREATE INDEX idx_maintenance_records_status ON maintenance_records (status);
CREATE INDEX idx_maintenance_records_block ON maintenance_records (block_id);
