

CREATE TABLE taxi_zones (
    locationid INTEGER PRIMARY KEY,
    borough TEXT,
    zone TEXT,
    service_zone TEXT
);
CREATE TABLE vendors (
    vendor_id INT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE rate_codes (
    rate_code_id INT PRIMARY KEY,
    description TEXT
);

INSERT INTO vendors VALUES
(1, 'Creative Mobile Technologies, LLC'),
(2, 'Curb Mobility, LLC'),
(6, 'Myle Technologies Inc'),
(7, 'Helix');

INSERT INTO rate_codes VALUES
(1, 'Standard rate'),
(2, 'JFK'),
(3, 'Newark'),
(4, 'Nassau or Westchester'),
(5, 'Negotiated fare'),
(6, 'Group ride'),
(99, 'Null/unknown');

INSERT INTO payment_types VALUES
(0, 'Flex Fare trip'),
(1, 'Credit card'),
(2, 'Cash'),
(3, 'No charge'),
(4, 'Dispute'),
(5, 'Unknown'),
(6, 'Voided trip');


CREATE TABLE trips (
    trip_id SERIAL PRIMARY KEY,

    vendor_id INT,
    tpep_pickup_datetime TIMESTAMP,
    tpep_dropoff_datetime TIMESTAMP,
    passenger_count BIGINT,
    trip_distance DOUBLE PRECISION,
    rate_code_id BIGINT,
    store_and_fwd_flag CHAR(1),
    pu_location_id INT,
    do_location_id INT,
    payment_type_id BIGINT,
    fare_amount DOUBLE PRECISION,
    extra DOUBLE PRECISION,
    mta_tax DOUBLE PRECISION,
    tip_amount DOUBLE PRECISION,
    tolls_amount DOUBLE PRECISION,
    improvement_surcharge DOUBLE PRECISION,
    total_amount DOUBLE PRECISION,
    congestion_surcharge DOUBLE PRECISION,
    airport_fee DOUBLE PRECISION,

    -- Foreign Key Constraints
    CONSTRAINT fk_vendor
      FOREIGN KEY (vendor_id)
      REFERENCES vendors (vendor_id),

    CONSTRAINT fk_rate_code
      FOREIGN KEY (rate_code_id)
      REFERENCES rate_codes (rate_code_id),

    CONSTRAINT fk_payment_type
      FOREIGN KEY (payment_type_id)
      REFERENCES payment_types (payment_type_id),

    CONSTRAINT fk_pu_location
      FOREIGN KEY (pu_location_id)
      REFERENCES locations (locationid),

    CONSTRAINT fk_do_location
      FOREIGN KEY (do_location_id)
      REFERENCES locations (locationid)
);

CREATE TABLE trips_enriched_summary (
    trip_id INT PRIMARY KEY,
    tpep_pickup_datetime TIMESTAMP,
    tpep_dropoff_datetime TIMESTAMP,
    trip_distance DOUBLE PRECISION,
    trip_duration_minutes DOUBLE PRECISION,
    average_speed_mph DOUBLE PRECISION,

    CONSTRAINT fk_trip_id FOREIGN KEY (trip_id)
      REFERENCES trips (trip_id)
);