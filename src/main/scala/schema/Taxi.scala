package schema

object Taxi {
  case class TaxiZoneLookup(
                           LocationID:String,
                           Borough:String,
                           Zone:String,
                           service_zone:String
                           )
  case class Trip(
                   vendor_id: Int,
                   tpep_pickup_datetime: java.sql.Timestamp,
                   tpep_dropoff_datetime: java.sql.Timestamp,
                   passenger_count: Long,
                   trip_distance: Double,
                   rate_code_id: Long,
                   store_and_fwd_flag: String,
                   pu_location_id: Int,
                   do_location_id: Int,
                   payment_type_id: Long,
                   fare_amount: Double,
                   extra: Double,
                   mta_tax: Double,
                   tip_amount: Double,
                   tolls_amount: Double,
                   improvement_surcharge: Double,
                   total_amount: Double,
                   congestion_surcharge: Double,
                   airport_fee: Double
                 )

}
