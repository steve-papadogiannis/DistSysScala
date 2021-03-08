package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.DirectionsResultWrapper

case class DBResult(calculateDirections: CalculateDirections, directionsResultWrappers: List[DirectionsResultWrapper])
