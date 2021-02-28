package gr.papadogiannis.stefanos.models

import com.google.maps.model.DirectionsResult

sealed trait ReducerResult

final case class ConcreteReducerResult(valueOption: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult

case object ReducerNotAvailable extends ReducerResult

case object ReducerTimedOut extends ReducerResult
