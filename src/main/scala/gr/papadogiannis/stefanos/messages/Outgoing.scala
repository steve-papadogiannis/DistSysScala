package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.DirectionsResult

final case class Outgoing(name: String, directionsResult: DirectionsResult)
