package gr.papadogiannis.stefanos.constants

object ApplicationConstants {
  val GOOGLE_DIRECTIONS_API_ACTOR_NAME = "google-directions-api-actor"
  val START_POINT_LONGITUDE_FIELD_NAME = "Start Point Longitude"
  val START_POINT_LATITUDE_FIELD_NAME = "Start Point Latitude"
  val ACTOR_SYSTEM_NAME = "directions-map-reduce-actor-system"
  val END_POINT_LONGITUDE_FIELD_NAME = "End Point Longitude"
  val END_POINT_LATITUDE_FIELD_NAME = "End Point Latitude"
  val REDUCERS_GROUP_ACTOR_NAME = "reducers-group-actor"
  val RECEIVED_MESSAGE_PATTERN = "Received message: %s"
  val MAPPERS_GROUP_ACTOR_NAME = "mappers-group-actor"
  val MEM_CACHE_ACTOR_NAME = "mem-cache-actor"
  val DIRECTIONS_API_KEY = "directionsApiKey"
  val MONGO_ACTOR_NAME = "mongo-actor"
  val DEFAULT_HOST_NAME = "localhost"
  val SUPERVISOR_NAME = "supervisor"
  val LONGITUDE_LOWER_BOUND = -180d
  val LONGITUDE_UPPER_BOUND = 180d
  val LATITUDE_LOWER_BOUND = -85d
  val LATITUDE_UPPER_BOUND = 85d
  val NEW_LINE_SEPARATOR = "\n"
  val DECIMAL_FORMAT = "###.##"
  val SERVER_NAME = "server"
  val MASTER_NAME = "master"
  val DEFAULT_PORT = 8383
  val API_KEY = "API_KEY"
}
