class GeoPoint(latitude: Double, longitude: Double) {
  def getLatitude: Double = latitude
  def getLongitude: Double = longitude
  def euclideanDistance(other: GeoPoint): Double = Math.sqrt(Math.pow(latitude - other.latitude, 2) + Math.pow(longitude - other.longitude, 2))
  override def toString: String = "GeoPoint [ Latitude = " + latitude + ", Longitude = " + longitude + "]"
}
