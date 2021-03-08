package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

final case class Handle(requestId: Long, geoPointPair: GeoPointPair, complete: DirectionsResult => Unit)
