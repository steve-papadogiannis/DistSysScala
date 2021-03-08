package gr.papadogiannis.stefanos.reducers

import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}
import gr.papadogiannis.stefanos.messages._

import scala.concurrent.duration.FiniteDuration

object ReducersGroupQuery {
  def props(actorToReducerId: Map[ActorRef, String],
            request: CalculateReduction,
            requester: ActorRef,
            timeout: FiniteDuration): Props =
    Props(new ReducersGroupQuery(actorToReducerId, request, requester, timeout))
}

class ReducersGroupQuery(actorToReducerId: Map[ActorRef, String],
                         request: CalculateReduction,
                         requester: ActorRef,
                         timeout: FiniteDuration) extends Actor with ActorLogging {

  import context.dispatcher

  val queryTimeoutTimer: Cancellable = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToReducerId.keysIterator.foreach { reducerActor =>
      context.watch(reducerActor)
      reducerActor ! request
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }

  override def receive: Receive = waitingForReplies(Map.empty, actorToReducerId.keySet)

  def waitingForReplies(repliesSoFar: Map[String, ReducerResult],
                        stillWaiting: Set[ActorRef]): Receive = {
    case message@RespondReduceResult(_, valueOption) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val reducerActor = sender()
      val reading = ConcreteReducerResult(valueOption)
      receivedResponse(reducerActor, reading, stillWaiting, repliesSoFar)
    case message@Terminated(reducerActor) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      receivedResponse(reducerActor, ReducerNotAvailable, stillWaiting, repliesSoFar)
    case CollectionTimeout =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(CollectionTimeout))
      val timedOutReplies =
        stillWaiting.map { reducerActor =>
          val reducerId = actorToReducerId(reducerActor)
          reducerId -> ReducerTimedOut
        }
      requester ! RespondAllReduceResults(request, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(reducerActor: ActorRef,
                       reading: ReducerResult,
                       stillWaiting: Set[ActorRef],
                       repliesSoFar: Map[String, ReducerResult]): Unit = {
    context.unwatch(reducerActor)
    val reducerId = actorToReducerId(reducerActor)
    val newStillWaiting = stillWaiting - reducerActor
    val newRepliesSoFar = repliesSoFar + (reducerId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! RespondAllReduceResults(request, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }

}
