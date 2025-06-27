package com.bosch.asc.pipeline

import org.scalatest.funsuite.AnyFunSuite
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

class TaxiPipelineTest extends AnyFunSuite {

  test("Simple enrichment logic") {
    // Create SparkSession
    val spark = SparkSession.builder()
      .appName("SimpleTaxiPipelineTest")
      .master("local[*]")
      .getOrCreate()

    // Import implicits
    import spark.implicits._

    // Sample input data
    val inputDF = Seq(
      ("2023-01-01 08:00:00", "2023-01-01 08:30:00", 15.0),
      ("2023-01-01 09:00:00", "2023-01-01 09:10:00", 2.0)
    ).toDF("tpep_pickup_datetime", "tpep_dropoff_datetime", "trip_distance")
      .withColumn("tpep_pickup_datetime", to_timestamp($"tpep_pickup_datetime"))
      .withColumn("tpep_dropoff_datetime", to_timestamp($"tpep_dropoff_datetime"))

    // Transformation (same as your enrichment logic)
    val enrichedDF = inputDF
      .withColumn("trip_duration_minutes",
        (unix_timestamp($"tpep_dropoff_datetime") - unix_timestamp($"tpep_pickup_datetime")) / 60)
      .withColumn("average_speed_mph",
        round($"trip_distance" / ($"trip_duration_minutes" / 60), 2))

    // Collect results
    val result = enrichedDF.select("trip_duration_minutes", "average_speed_mph").collect()

    // Assertions
    assert(result(0).getDouble(0) == 30.0)  // 30 minutes
    assert(result(0).getDouble(1) == 30.0)  // 30 mph

    assert(result(1).getDouble(0) == 10.0)  // 10 minutes
    assert(result(1).getDouble(1) == 12.0)  // 12 mph

    // Stop Spark
    spark.stop()
  }
}
