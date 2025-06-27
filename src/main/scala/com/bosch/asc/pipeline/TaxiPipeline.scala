package com.bosch.asc.pipeline

import org.apache.spark.SparkConf
import org.apache.spark.internal.Logging
import org.apache.spark.sql.{Encoders, SparkSession}
import org.apache.spark.sql.functions._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import schema.Taxi.{TaxiZoneLookup, Trip}

/**
 * object containing main method for this application.
 * contains sample starter code
 *
 * TODO: replace this with your implementation
 */
object TaxiPipeline extends Logging {

  /**
   * entry point for the pipeline
   *
   * @param args command line arguments
   */
  def main(args: Array[String]): Unit = {

    // conf is loaded from resources/application.conf
    val conf: PipelineProperties = ConfigSource
      .default
      .loadOrThrow[PipelineProperties]
    val extraConf = new SparkConf()
      .set("spark.driver.extraJavaOptions", "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED")
      .set("spark.executor.extraJavaOptions", "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED")
    // init spark session
    val spark = SparkSession.builder()
      .appName("taxi-data-pipeline")
      .config(extraConf)
      .config("spark.hadoop.fs.s3a.path.style.access", "true")
      .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("fs.s3a.endpoint", conf.minioEndpoint)
      .config("fs.s3a.access.key", conf.minioAccessKey)
      .config("fs.s3a.secret.key", conf.minioSecretKey)
      .master("local[*]")
      .getOrCreate()

    // TODO: this is just an example
    val exampleCols: Seq[String] = Seq(
      "trip_id",
      "VendorID",
      "tpep_pickup_datetime",
      "tpep_dropoff_datetime",
      "passenger_count",
      "fare_amount"
    )

    val df = spark
      .read
      .parquet("s3a://taxi-bucket/taxi-data/")
      .withColumn("trip_id", uuid().cast("string"))
      .select(exampleCols.map(col): _*)
      .limit(100)

    // write the DataFrame to PostgreSQL
    df.write
      .mode("append")
      .jdbc(
        url = conf.jdbcUrl,
        table = "test_taxi_table",
        connectionProperties = conf.connectionProperties
      )

    // Lookup table write to lookup table taxi_zones
    import spark.implicits._
    val lookupMapping = Encoders.product[TaxiZoneLookup]
    val lookup_df = spark.read.format("csv").option("header", "true")
      .load("s3a://taxi-bucket/reference-data/")
      .withColumn("locationid", col("LocationID").cast("int"))
      .select(lookupMapping.schema.map(x => col(x.name)):_*)
      .as[TaxiZoneLookup](lookupMapping)

    lookup_df.write
          .mode("append")
          .jdbc(
            url = conf.jdbcUrl,
            table = "taxi_zones",
            connectionProperties = conf.connectionProperties
          )

    val tripsMapping = Encoders.product[Trip]
    val final_df = spark.read
          .parquet("s3a://taxi-bucket/taxi-data/")
          .withColumnRenamed("VendorID", "vendor_id")
          .withColumnRenamed("RatecodeID", "rate_code_id")
          .withColumnRenamed("PULocationID", "pu_location_id")
          .withColumnRenamed("DOLocationID", "do_location_id")
          .withColumnRenamed("payment_type", "payment_type_id")
          .withColumnRenamed("Airport_fee", "airport_fee")
          .select(tripsMapping.schema.map(x => col(x.name)):_*)

    final_df.write
              .mode("append")
              .jdbc(
                url = conf.jdbcUrl,
                table = "trips",
                connectionProperties = conf.connectionProperties
              )
    val tripsDF = spark.read.jdbc(
      url = conf.jdbcUrl,
      table = "trips",
      properties = conf.connectionProperties
    ).select(
      col("trip_id"),
      col("tpep_pickup_datetime"),
      col("tpep_dropoff_datetime"),
      col("trip_distance")
    ).filter(
      col("tpep_pickup_datetime").isNotNull && col("trip_distance") > 0
    )
    // Enrichment
    val enrichedDF = tripsDF
      .withColumn("trip_duration_minutes",
        (unix_timestamp(col("tpep_dropoff_datetime")) - unix_timestamp(col("tpep_pickup_datetime"))) / 60)
      .withColumn("average_speed_mph",
        round(col("trip_distance") / (col("trip_duration_minutes") / 60), 2))

    // Write to `trips_enriched_summary` table using .jdbc
    enrichedDF.write
      .mode("overwrite") // or "append"
      .jdbc(
        url = conf.jdbcUrl,
        table = "trips_enriched_summary",
        connectionProperties = conf.connectionProperties
      )
    // Stop SparkSession
    spark.stop()
  }
}