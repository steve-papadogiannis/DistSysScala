package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{GeoPointPair, LatLng}

final case class Handle(requestId: Long, geoPointPair: GeoPointPair, complete: List[LatLng] => Unit)
