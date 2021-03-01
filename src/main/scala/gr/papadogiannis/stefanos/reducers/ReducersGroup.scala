package gr.papadogiannis.stefanos.reducers

import gr.papadogiannis.stefanos.models.{CalculateReduction, ReducerRegistered, ReplyReducerList, RequestReducerList, RequestTrackReducer}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

object ReducersGroup {
  def props(): Props = Props(new ReducersGroup)
}

class ReducersGroup extends Actor with ActorLogging {

  var reducerNameToReducerActorRef = Map.empty[String, ActorRef]

  var reducerActorRefToReducerName = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("ReducersGroup started")

  override def postStop(): Unit = log.info("ReducersGroup stopped")

  override def receive: Receive = {
    case message@RequestTrackReducer(reducerName) =>
      val reducerActor = reducerNameToReducerActorRef.get(reducerName) match {
        case Some(reducerActor) => reducerActor
        case None =>
          log.info("Creating reducer actor [{}]", reducerName)
          val reducerActor = context.actorOf(ReducerWorker.props(reducerName), s"$reducerName")
          reducerActor
      }
      reducerActor ! message
    case Terminated(reducerActor) =>
      val reducerName = reducerActorRefToReducerName(reducerActor)
      log.info("Reducer actor {} has been terminated", reducerName)
      reducerActorRefToReducerName -= reducerActor
      reducerNameToReducerActorRef -= reducerName
    case RequestReducerList(requestId) =>
      sender() ! ReplyReducerList(requestId, reducerNameToReducerActorRef.keySet)
    case request@CalculateReduction(requestId, _) =>
      context.actorOf(ReducersGroupQuery.props(reducerActorRefToReducerName, request, sender(), 5.minutes), s"reducers-group-query-$requestId")
    case ReducerRegistered(reducerName) =>
      log.info("Registering Reducer Actor [{}]", reducerName)
      reducerNameToReducerActorRef += reducerName -> sender()
      reducerActorRefToReducerName += sender() -> reducerName
  }

}

