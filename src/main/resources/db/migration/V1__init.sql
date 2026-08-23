-- USERS
CREATE TABLE users (
                       id              BIGSERIAL PRIMARY KEY,
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       role            VARCHAR(20)  NOT NULL CHECK (role IN ('ORGANIZER', 'CUSTOMER')),
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- EVENTS
CREATE TABLE events (
                        id              BIGSERIAL PRIMARY KEY,
                        organizer_id    BIGINT NOT NULL REFERENCES users(id),
                        name            VARCHAR(255) NOT NULL,
                        venue           VARCHAR(255) NOT NULL,
                        event_time      TIMESTAMPTZ NOT NULL,
                        total_seats     INTEGER NOT NULL CHECK (total_seats >= 0),
                        available_seats INTEGER NOT NULL CHECK (available_seats >= 0),
                        price           NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
                        version         BIGINT NOT NULL DEFAULT 0,   -- optimistic locking
                        created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                        updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_organizer_id ON events(organizer_id);
CREATE INDEX idx_events_event_time   ON events(event_time);

-- BOOKINGS
CREATE TABLE bookings (
                          id              BIGSERIAL PRIMARY KEY,
                          event_id        BIGINT NOT NULL REFERENCES events(id),
                          customer_id     BIGINT NOT NULL REFERENCES users(id),
                          seats_booked    INTEGER NOT NULL CHECK (seats_booked > 0),
                          status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
                              CHECK (status IN ('CONFIRMED', 'CANCELLED')),
                          booked_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bookings_event_id    ON bookings(event_id);
CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);

-- Prevents a customer from having two CONFIRMED bookings on the same event,
-- but a CANCELLED one doesn't block a rebook (this is the "partial" part)
CREATE UNIQUE INDEX uq_customer_event_confirmed
    ON bookings(customer_id, event_id)
    WHERE status = 'CONFIRMED';