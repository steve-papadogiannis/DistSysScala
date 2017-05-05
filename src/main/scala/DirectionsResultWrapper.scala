import com.google.maps.model.DirectionsResult
/**
  * Created by stefanos on 5/5/17.
  */
class DirectionsResultWrapper {

  private var id: String = _

  private var startPoint: GeoPoint = _

  private var endPoint: GeoPoint = _

  private var directionsResult: DirectionsResult = _

  def DirectionsResultWrapper(startPoint: GeoPoint, endPoint: GeoPoint, directionsResult: DirectionsResult) {
    this.startPoint = startPoint
    this.endPoint = endPoint
    this.directionsResult = directionsResult
  }

  def setStartPoint(startPoint: GeoPoint): Unit = {
    this.startPoint = startPoint
  }

  def setEndPoint(endPoint: GeoPoint): Unit = {
    this.endPoint = endPoint
  }

  def getId: String = id

  def setId(id: String): Unit = {
    this.id = id
  }

  def setDirectionsResult(directionsResult: DirectionsResult): Unit = {
    this.directionsResult = directionsResult
  }

  def getStartPoint: GeoPoint = startPoint

  def getEndPoint: GeoPoint = endPoint

  def getDirectionsResult: DirectionsResult = directionsResult
}
