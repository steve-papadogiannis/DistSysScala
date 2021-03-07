package gr.papadogiannis.stefanos.models

case class GeoPoint(latitude: Double, longitude: Double) {

  def euclideanDistance(other: GeoPoint): Double =
    Math.sqrt(Math.pow(latitude - other.latitude, 2) +
      Math.pow(longitude - other.longitude, 2))

}
