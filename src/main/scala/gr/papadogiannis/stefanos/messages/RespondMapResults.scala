package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

final case class RespondMapResults(request: CalculateDirections, valueOption: List[Map[GeoPointPair, DirectionsResult]])
