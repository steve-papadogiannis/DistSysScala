package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.DirectionsResult

final case class Handle(requestId: Long, startLat: Double, startLong: Double, endLat: Double, endLong: Double, complete: DirectionsResult => Unit)
