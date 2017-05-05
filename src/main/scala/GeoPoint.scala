/**
  * Created by stefanos on 5/5/17.
  */
class GeoPoint {
  private var latitude = 0.0
  private var longitude = 0.0

  def GeoPoint(latitude: Double, longitude: Double) {
    this.latitude = latitude
    this.longitude = longitude
  }

  def getLatitude: Double = latitude

  def getLongitude: Double = longitude

  def setLatitude(latitude: Double): Unit = {
    this.latitude = latitude
  }

  def setLongitude(longitude: Double): Unit = {
    this.longitude = longitude
  }

  def euclideanDistance(other: GeoPoint): Double = Math.sqrt(Math.pow(latitude - other.latitude, 2) + Math.pow(longitude - other.longitude, 2))

  override def toString: String = "GeoPoint [ Latitude = " + latitude + ", Longitude = " + longitude + "]"

}
