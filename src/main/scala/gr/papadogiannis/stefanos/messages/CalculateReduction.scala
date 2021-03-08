package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

final case class CalculateReduction(request: CalculateDirections, merged: List[Map[GeoPointPair, DirectionsResult]])
