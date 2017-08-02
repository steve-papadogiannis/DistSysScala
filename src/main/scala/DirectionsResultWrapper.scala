import com.google.maps.model.DirectionsResult

class DirectionsResultWrapper(idP: String, startPointP: GeoPoint, endPointP: GeoPoint,
                             directionsResultP: DirectionsResult) {
  def id: String = idP
  def startPoint: GeoPoint = startPointP
  def endPoint: GeoPoint = endPointP
  def directionsResult: DirectionsResult = directionsResultP
}
