package gr.papadogiannis.stefanos.reducers

import ReducersGroup.{CalculateReduction, ReplyReducerList, RequestReducerList}
import gr.papadogiannis.stefanos.masters.Master.RequestTrackReducer
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.google.maps.model.DirectionsResult
import gr.papadogiannis.stefanos.models.GeoPointPair

import scala.concurrent.duration._

object ReducersGroup {

  def props(mappersGroupActorRef: ActorRef, masterActorRef: ActorRef): Props = Props(new ReducersGroup)

  final case class RequestReducerList(requestId: Long)

  final case class ReplyReducerList(requestId: Long, ids: Set[String])

  sealed trait ReducerResult

  final case class Result(value: Double) extends ReducerResult

  case object ResultNotAvailable extends ReducerResult

  case object ReducerNotAvailable extends ReducerResult

  case object ReducerTimedOut extends ReducerResult

  final case class RequestAllReduceResults(requestId: Long)

  final case class RespondAllReduceResults(request: CalculateReduction, results: Map[String, ReducerResult])

  case class MapResult(value: Any)

  case class CalculateReduction(requestId: Long, merged: List[Map[GeoPointPair, DirectionsResult]])

  case class ConcreteResult(valueOption: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult

}

class ReducersGroup
  extends Actor
    with ActorLogging {

  var reducerIdToActor = Map.empty[String, ActorRef]

  var actorToReducerId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("ReducersGroup started")

  override def postStop(): Unit = log.info("ReducersGroup stopped")

  override def receive: Receive = {
    case request@RequestTrackReducer(reducerId) =>
      reducerIdToActor.get(reducerId) match {
        case Some(reducerActor) =>
          reducerActor forward request
        case None =>
          log.info("Creating reducer actor [{}]", reducerId)
          val reducerActor = context.actorOf(ReduceWorker.props(reducerId), s"$reducerId")
          reducerIdToActor += reducerId -> reducerActor
          actorToReducerId += reducerActor -> reducerId
          reducerActor forward request
      }
    case Terminated(reducerActor) =>
      val reducerId = actorToReducerId(reducerActor)
      log.info("Reducer actor {} has been terminated", reducerId)
      actorToReducerId -= reducerActor
      reducerIdToActor -= reducerId
    case RequestReducerList(requestId) =>
      sender() ! ReplyReducerList(requestId, reducerIdToActor.keySet)
    case request@CalculateReduction(requestId, _) =>
      context.actorOf(ReducersGroupQuery.props(actorToReducerId, request, sender(), 5.minutes), s"reducers-group-query-$requestId")
  }

}

