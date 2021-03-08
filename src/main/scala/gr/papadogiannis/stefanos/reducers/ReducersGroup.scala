package gr.papadogiannis.stefanos.reducers

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages._

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
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val reducerActor = reducerNameToReducerActorRef.get(reducerName) match {
        case Some(reducerActor) => reducerActor
        case None =>
          log.info("Creating reducer actor [{}]", reducerName)
          val reducerActor = context.actorOf(ReducerWorker.props(reducerName), s"$reducerName")
          reducerActor
      }
      reducerActor ! message
    case message@Terminated(reducerActor) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val reducerName = reducerActorRefToReducerName(reducerActor)
      log.info("Reducer actor {} has been terminated", reducerName)
      reducerActorRefToReducerName -= reducerActor
      reducerNameToReducerActorRef -= reducerName
    case message@RequestReducerList(requestId) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      sender() ! ReplyReducerList(requestId, reducerNameToReducerActorRef.keySet)
    case request@CalculateReduction(calculateDirections, _) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      context.actorOf(ReducersGroupQuery.props(reducerActorRefToReducerName, request, sender(), 5.minutes), s"reducers-group-query-${calculateDirections.requestId}")
    case message@ReducerRegistered(reducerName) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      log.info("Registering Reducer Actor [{}]", reducerName)
      reducerNameToReducerActorRef += reducerName -> sender()
      reducerActorRefToReducerName += sender() -> reducerName
  }

}

