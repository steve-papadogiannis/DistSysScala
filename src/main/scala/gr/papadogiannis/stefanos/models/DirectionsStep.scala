package gr.papadogiannis.stefanos.models

case class DirectionsStep(startLocation: LatLng, endLocation: LatLng, polyline: EncodedPolyline) {
  override def toString: String = s"DirectionsStep(${startLocation.toString}, ${endLocation.toString}, <polyline gibberish...>)"
}
