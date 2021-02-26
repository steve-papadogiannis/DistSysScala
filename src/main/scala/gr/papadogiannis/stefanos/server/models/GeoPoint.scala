package gr.papadogiannis.stefanos.server.models

class GeoPoint(latitude: Double, longitude: Double) {

  def getLatitude: Double = latitude

  def getLongitude: Double = longitude

  def euclideanDistance(other: GeoPoint): Double =
    Math.sqrt(Math.pow(getLatitude - other.getLatitude, 2) + Math.pow(getLongitude - other.getLongitude, 2))

  override def toString: String = "GeoPoint [ Latitude = " + latitude + ", Longitude = " + longitude + "]"

}
