package gr.papadogiannis.stefanos.models

final case class Handle(requestId: Long, startLat: Double, startLong: Double, endLat: Double, endLong: Double, complete: DirectionsResult => Unit)