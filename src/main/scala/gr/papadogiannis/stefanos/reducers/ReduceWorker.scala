package gr.papadogiannis.stefanos.reducers

import ReduceWorker.{ReducerRegistered, RespondReduceResult}
import akka.actor.{Actor, ActorLogging, Props}
import com.google.maps.model.DirectionsResult
import gr.papadogiannis.stefanos.models.{CalculateReduction, GeoPointPair, RequestTrackReducer}

object ReduceWorker {

  def props(reducerId: String): Props = Props(new ReduceWorker(reducerId))

  case class ReadReduceResult(i: Int)

  sealed trait ReducerResult

  case class RespondReduceResult(requestId: Long, value: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult

  object ReducerRegistered

}

class ReduceWorker(name: String)
  extends Actor
    with ActorLogging {

  override def preStart(): Unit = log.info("Reducer actor {} started", name)

  override def postStop(): Unit = log.info("Reducer actor {} stopped", name)

  override def receive: Receive = {
    case RequestTrackReducer(_) =>
      sender() ! ReducerRegistered
    case request@CalculateReduction(requestId, merged) =>
      val finalResult = reduce(merged)
      sender() ! RespondReduceResult(requestId, finalResult)
  }

  import com.google.maps.model.DirectionsResult

  def reduce(incoming: List[Map[GeoPointPair, DirectionsResult]]): Map[GeoPointPair, List[DirectionsResult]] = {
    incoming.foldLeft(Map.empty[GeoPointPair, List[DirectionsResult]])((accumulator, x) => {
      val geoPointPair = x.keySet.head
      val list = List.empty[DirectionsResult]
      x.getOrElse(geoPointPair, new DirectionsResult()) :: list
      if (accumulator.contains(geoPointPair))
        accumulator + (geoPointPair ->
          (accumulator.getOrElse(geoPointPair, List.empty[DirectionsResult]) :: list).asInstanceOf[List[DirectionsResult]])
      else
        accumulator + (geoPointPair -> list)
    }
    )
  }

}
