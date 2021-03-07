package gr.papadogiannis.stefanos.models

final case class CalculateReduction(requestId: Long, merged: List[Map[GeoPointPair, DirectionsResult]])

