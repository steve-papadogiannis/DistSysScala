package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

final case class Handle(requestId: Long, startLat: Double, startLong: Double, endLat: Double, endLong: Double, complete: DirectionsResult => Unit)