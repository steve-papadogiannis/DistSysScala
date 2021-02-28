package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

final case class CalculateReduction(requestId: Long, merged: List[Map[GeoPointPair, DirectionsResult]])

