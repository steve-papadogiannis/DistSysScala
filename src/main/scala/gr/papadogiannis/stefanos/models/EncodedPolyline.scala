package gr.papadogiannis.stefanos.models

import com.google.maps.internal.PolylineEncoding

import scala.collection.JavaConverters._

case class EncodedPolyline(points: String) {

  def decodePath: List[LatLng] = PolylineEncoding
    .decode(points)
    .asScala
    .map(latLng => LatLng(latLng.lat, latLng.lng))
    .toList

}
