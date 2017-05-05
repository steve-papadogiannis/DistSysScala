/**
  * Created by stefanos on 5/5/17.
  */
class GeoPointPair {
  private var startGeoPoint: GeoPoint = _
  private var endGeoPoint: GeoPoint = _

  def GeoPointPair(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint) {
    this.startGeoPoint = startGeoPoint
    this.endGeoPoint = endGeoPoint
  }

  def getStartGeoPoint: GeoPoint = startGeoPoint

  def getEndGeoPoint: GeoPoint = endGeoPoint

}
