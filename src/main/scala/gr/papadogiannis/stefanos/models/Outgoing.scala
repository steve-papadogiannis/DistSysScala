package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

final case class Outgoing(name: String, directionsResult: DirectionsResult)
