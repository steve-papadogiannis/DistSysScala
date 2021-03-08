package gr.papadogiannis.stefanos.reducers

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import akka.actor.{Actor, ActorLogging, Props}
import gr.papadogiannis.stefanos.messages.{CalculateReduction, ReducerRegistered, RequestTrackReducer, RespondReduceResult}

object ReducerWorker {
  def props(reducerId: String): Props = Props(new ReducerWorker(reducerId))
}

class ReducerWorker(name: String) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("Reducer actor {} started", name)

  override def postStop(): Unit = log.info("Reducer actor {} stopped", name)

  override def receive: Receive = {
    case message@RequestTrackReducer(reducerName) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      sender() ! ReducerRegistered(reducerName)
    case message@CalculateReduction(requestId, merged) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val finalResult = reduce(merged)
      sender() ! RespondReduceResult(requestId, finalResult)
  }

  def reduce(incoming: List[Map[GeoPointPair, DirectionsResult]]): Map[GeoPointPair, List[DirectionsResult]] = {
    incoming.foldLeft(Map.empty[GeoPointPair, List[DirectionsResult]])((accumulator, x) => {
      val geoPointPair = x.keySet.head
      val list = List.empty[DirectionsResult]
      x.getOrElse(geoPointPair, DirectionsResult) :: list
      if (accumulator.contains(geoPointPair))
        accumulator + (geoPointPair ->
          (accumulator.getOrElse(geoPointPair, List.empty[DirectionsResult]) :: list).asInstanceOf[List[DirectionsResult]])
      else
        accumulator + (geoPointPair -> list)
    }
    )
  }

}
