package gr.papadogiannis.stefanos.models

sealed trait ReducerResult

final case class RespondReduceResult(requestId: Long, value: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult

final case class ConcreteReducerResult(valueOption: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult

case object ReducerNotAvailable extends ReducerResult

case object ReducerTimedOut extends ReducerResult
