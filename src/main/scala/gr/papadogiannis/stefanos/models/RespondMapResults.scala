package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

case class RespondMapResults(request: CalculateDirections, valueOption: List[Map[GeoPointPair, DirectionsResult]])
