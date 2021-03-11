package gr.papadogiannis.stefanos.models

case class DirectionsLeg(startLocation: LatLng, endLocation: LatLng, steps: List[DirectionsStep], duration: Duration)
