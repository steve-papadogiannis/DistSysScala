package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.GeoPointPair

final case class Handle(requestId: Long, geoPointPair: GeoPointPair, complete: List[Double] => Unit)
