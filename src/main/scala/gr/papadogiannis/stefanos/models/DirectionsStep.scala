package gr.papadogiannis.stefanos.models

case class DirectionsStep(polyline: EncodedPolyline) {
  override def toString: String = s"DirectionsStep(<polyline gibberish...>)"
}
