package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

sealed trait MapperResult

final case class ConcreteMapperResult(value: List[Map[GeoPointPair, DirectionsResult]]) extends MapperResult

case object MapperNotAvailable extends MapperResult

case object MapperTimedOut extends MapperResult
