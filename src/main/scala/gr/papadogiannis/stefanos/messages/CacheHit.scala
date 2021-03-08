package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.DirectionsResult

case class CacheHit(calculateDirections: CalculateDirections, directionsResult: DirectionsResult) extends CacheResult
