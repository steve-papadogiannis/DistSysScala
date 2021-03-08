package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.GeoPointPair

final case class CalculateDirections(requestId: Long, geoPointPair: GeoPointPair)
