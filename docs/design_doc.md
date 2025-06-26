# NYC Taxi Trips Data Pipeline Design Document

## 1. Objective

This project aims to design and implement a data pipeline that:
- Reads NYC taxi trip data from Parquet files.
- Transforms and enriches the data using Apache Spark.
- Loads the processed data into a PostgreSQL database.
- Supports analytical workloads for optimizing taxi traffic and urban mobility decisions.

---

## 2. Technology Stack

| Component     | Technology                            |
|---------------|---------------------------------------|
| Processing    | Apache Spark                          |
| Storage       | Parquet (source), PostgreSQL (target) |
| Orchestration | (Optional: Apache Airflow / cron)     |
| Language      | Scala (Scala Spark)                   |
| Hosting       | GitHub                                |

---

## 3. High-Level Architecture

```text
          +-------------------+
          |   Parquet Files   |
          +--------+----------+
                   |
                   v
          +--------+----------+
          |  Spark ETL Job    |
          |  (Transformations)|
          +--------+----------+
                   |
                   v
         +---------+----------+
         |   PostgreSQL DB    |
         | (Trips + Lookups)  |
         +--------------------+
```
---
## 4. Data Model

### 4.1 `trips` Table

| Column Name             | Data Type         | Description                              |
|-------------------------|-------------------|------------------------------------------|
| trip_id                 | SERIAL (PK)       | Unique identifier for each trip          |
| vendor_id               | INT               | Refers to the vendor of the trip         |
| tpep_pickup_datetime    | TIMESTAMP         | Trip pickup timestamp                    |
| tpep_dropoff_datetime   | TIMESTAMP         | Trip dropoff timestamp                   |
| passenger_count         | BIGINT            | Number of passengers                     |
| trip_distance           | DOUBLE PRECISION  | Distance covered during the trip (miles)|
| rate_code_id            | BIGINT            | Refers to rate code                      |
| store_and_fwd_flag      | CHAR(1)           | Storage flag                             |
| pu_location_id          | INT               | Pickup location ID                       |
| do_location_id          | INT               | Dropoff location ID                      |
| payment_type_id         | BIGINT            | Refers to payment method                 |
| fare_amount             | DOUBLE PRECISION  | Base fare amount                         |
| extra                   | DOUBLE PRECISION  | Additional charges                       |
| mta_tax                 | DOUBLE PRECISION  | MTA tax                                  |
| tip_amount              | DOUBLE PRECISION  | Tip amount                               |
| tolls_amount            | DOUBLE PRECISION  | Toll charges                             |
| improvement_surcharge   | DOUBLE PRECISION  | Improvement surcharge                    |
| total_amount            | DOUBLE PRECISION  | Total fare including all charges         |
| congestion_surcharge    | DOUBLE PRECISION  | Congestion-related surcharge             |
| airport_fee             | DOUBLE PRECISION  | Additional fee for airport trips         |

**Foreign Keys:**
- `vendor_id` → `vendors(vendor_id)`
- `rate_code_id` → `rate_codes(rate_code_id)`
- `payment_type_id` → `payment_types(payment_type_id)`
- `pu_location_id` → `locations(locationid)`
- `do_location_id` → `locations(locationid)`

---

### 4.2 Lookup Tables

#### `vendors`

| Column Name | Data Type | Description       |
|-------------|-----------|-------------------|
| vendor_id   | INT (PK)  | Vendor identifier |
| vendor_name | TEXT      | Vendor name       |

#### `rate_codes`

| Column Name  | Data Type | Description                  |
|--------------|-----------|------------------------------|
| rate_code_id | BIGINT (PK)| Rate code identifier        |
| description  | TEXT      | Description of rate code     |

#### `payment_types`

| Column Name     | Data Type | Description                |
|-----------------|-----------|----------------------------|
| payment_type_id | BIGINT (PK)| Payment method identifier |
| description     | TEXT      | Type of payment (e.g. Card)|

#### `taxi_zones` (Taxi Zones)

| Column Name  | Data Type | Description               |
|--------------|-----------|---------------------------|
| locationid   | INT (PK)  | Unique zone ID            |
| borough      | TEXT      | Borough name              |
| zone         | TEXT      | Zone name                 |
| service_zone | TEXT      | Taxi service zone category|

ERD

```text
+------------------+          +------------------+
|    vendors       |          |   rate_codes     |
|------------------|          |------------------|
| vendor_id (PK)   |          | rate_code_id (PK)|
| vendor_name      |          | description      |
+------------------+          +------------------+

         ^                             ^
         |                             |
         |                             |
         |     +-----------------------------------------------+
         +-----|                    trips                       |
               |------------------------------------------------|
               | trip_id (PK)                                   |
               | vendor_id (FK → vendors.vendor_id)             |
               | tpep_pickup_datetime TIMESTAMP                 |
               | tpep_dropoff_datetime TIMESTAMP                |
               | passenger_count BIGINT                         |
               | trip_distance DOUBLE PRECISION                 |
               | rate_code_id (FK → rate_codes.rate_code_id)    |
               | store_and_fwd_flag CHAR(1)                     |
               | pu_location_id (FK → locations.locationid)     |
               | do_location_id (FK → locations.locationid)     |
               | payment_type_id (FK → payment_types.id)        |
               | fare_amount DOUBLE PRECISION                   |
               | extra DOUBLE PRECISION                         |
               | mta_tax DOUBLE PRECISION                       |
               | tip_amount DOUBLE PRECISION                    |
               | tolls_amount DOUBLE PRECISION                  |
               | improvement_surcharge DOUBLE PRECISION         |
               | total_amount DOUBLE PRECISION                  |
               | congestion_surcharge DOUBLE PRECISION          |
               | airport_fee DOUBLE PRECISION                   |
               +-----------------------------------------------+
                          ^       ^         ^
                          |       |         |
+------------------+      |       |         +----------------------+
| payment_types    |      |       |            |           locations |
|------------------|      |       |            ----------------------|
| payment_type_id  |<-----+       +------------->  | locationid (PK) |
| description      |                               | borough         |
+------------------+                               | zone            |
                                                   | service_zone    |
                                                   +----------------+

                          |
                          v

          +-----------------------------------------------+
          |           trips_enriched_summary              |
          |-----------------------------------------------|
          | trip_id (PK, FK → trips.trip_id)              |
          | tpep_pickup_datetime TIMESTAMP                |
          | tpep_dropoff_datetime TIMESTAMP               |
          | trip_distance DOUBLE PRECISION                |
          | trip_duration_minutes DOUBLE PRECISION        |
          | average_speed_mph DOUBLE PRECISION            |
          +-----------------------------------------------+

```

---

## 5. Spark Job

### 5.1 Spark Overview

- **Framework**: Apache Spark
- **Language**: Scala
- **Execution Mode**: Cluster or local
- **Input Format**: Parquet files
- **Output Format**: PostgreSQL (via JDBC)
- **Processing Type**: Batch