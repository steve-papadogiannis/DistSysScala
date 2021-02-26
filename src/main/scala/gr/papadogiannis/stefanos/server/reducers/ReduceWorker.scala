package gr.papadogiannis.stefanos.server.reducers

import gr.papadogiannis.stefanos.server.reducers.ReduceWorker.{ReducerRegistered, RespondReduceResult}
import gr.papadogiannis.stefanos.server.reducers.ReducersGroup.CalculateReduction
import gr.papadogiannis.stefanos.server.masters.Master.RequestTrackReducer
import gr.papadogiannis.stefanos.server.models.GeoPointPair
import akka.actor.{Actor, ActorLogging, Props}
import com.google.maps.model.DirectionsResult

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
