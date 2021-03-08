package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

final case class CalculateReduction(requestId: Long, merged: List[Map[GeoPointPair, DirectionsResult]])
