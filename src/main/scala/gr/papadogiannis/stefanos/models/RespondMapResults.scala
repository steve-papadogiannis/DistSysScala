package gr.papadogiannis.stefanos.models

final case class RespondMapResults(request: CalculateDirections, valueOption: List[Map[GeoPointPair, DirectionsResult]])
