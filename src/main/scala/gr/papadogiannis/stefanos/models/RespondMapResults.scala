package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

final case class RespondMapResults(request: CalculateDirections, valueOption: List[Map[GeoPointPair, DirectionsResult]])
