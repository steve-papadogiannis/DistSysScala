package gr.papadogiannis.stefanos.server.models

class GeoPointPair(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint) {
  def getStartGeoPoint: GeoPoint = startGeoPoint
  def getEndGeoPoint: GeoPoint = endGeoPoint
}
