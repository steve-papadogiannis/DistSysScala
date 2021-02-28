package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

case class CalculateReduction(requestId: Long, merged: List[Map[GeoPointPair, DirectionsResult]])

