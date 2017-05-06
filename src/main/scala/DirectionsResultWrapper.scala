import com.google.maps.model.DirectionsResult

/**
  * Created by stefanos on 5/5/17.
  */
class DirectionsResultWrapper(idP: String, startPointP: GeoPoint, endPointP: GeoPoint,
                             directionsResultP: DirectionsResult) {
  val id: String = idP
  val startPoint: GeoPoint = startPointP
  val endPoint: GeoPoint = endPointP
  val directionsResult: DirectionsResult = directionsResultP
}
