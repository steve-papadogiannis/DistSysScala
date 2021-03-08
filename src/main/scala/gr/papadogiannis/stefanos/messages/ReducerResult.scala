package gr.papadogiannis.stefanos.messages

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}

sealed trait ReducerResult

final case class RespondReduceResult(request: CalculateReduction, value: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult

final case class ConcreteReducerResult(valueOption: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult

case object ReducerNotAvailable extends ReducerResult

case object ReducerTimedOut extends ReducerResult
