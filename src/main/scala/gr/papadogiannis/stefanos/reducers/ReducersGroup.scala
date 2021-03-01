package gr.papadogiannis.stefanos.reducers

import gr.papadogiannis.stefanos.models.{CalculateReduction, ReplyReducerList, RequestReducerList, RequestTrackReducer}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

object ReducersGroup {
  def props(): Props = Props(new ReducersGroup)
}

class ReducersGroup extends Actor with ActorLogging {

  var reducerIdToActor = Map.empty[String, ActorRef]

  var actorToReducerId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("ReducersGroup started")

  override def postStop(): Unit = log.info("ReducersGroup stopped")

  override def receive: Receive = {
    case RequestTrackReducer(reducerId) =>
      val reducerActor = reducerIdToActor.get(reducerId) match {
        case Some(reducerActor) => reducerActor
        case None =>
          log.info("Creating reducer actor [{}]", reducerId)
          val reducerActor = context.actorOf(ReduceWorker.props(reducerId), s"$reducerId")
          reducerIdToActor += reducerId -> reducerActor
          actorToReducerId += reducerActor -> reducerId
          reducerActor
      }
      reducerActor ! RequestTrackReducer(reducerId)
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

