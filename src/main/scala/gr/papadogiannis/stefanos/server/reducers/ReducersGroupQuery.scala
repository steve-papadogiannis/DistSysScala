package gr.papadogiannis.stefanos.server.reducers

import gr.papadogiannis.stefanos.server.reducers.ReducersGroupQuery.CollectionTimeout
import gr.papadogiannis.stefanos.server.reducers.ReducersGroup.CalculateReduction
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

object ReducersGroupQuery {

  case object CollectionTimeout

  def props(actorToReducerId: Map[ActorRef, String], request: CalculateReduction,
            requester: ActorRef, timeout: FiniteDuration): Props =
    Props(new ReducersGroupQuery(actorToReducerId, request, requester, timeout))

}

class ReducersGroupQuery(actorToReducerId: Map[ActorRef, String], request: CalculateReduction,
                         requester: ActorRef, timeout: FiniteDuration)
  extends Actor
    with ActorLogging {

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

  def waitingForReplies(repliesSoFar: Map[String, ReducersGroup.ReducerResult],
                        stillWaiting: Set[ActorRef]): Receive = {
    case ReduceWorker.RespondeReduceResult(request, valueOption) =>
      val reducerActor = sender()
      val reading = ReducersGroup.ConcreteResult(valueOption)
      receivedResponse(reducerActor, reading, stillWaiting, repliesSoFar)
    case Terminated(reducerActor) =>
      receivedResponse(reducerActor, ReducersGroup.ReducerNotAvailable, stillWaiting, repliesSoFar)
    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { reducerActor =>
          val reducerId = actorToReducerId(reducerActor)
          reducerId -> ReducersGroup.ReducerTimedOut
        }
      requester ! ReducersGroup.RespondAllReduceResults(request, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(reducerActor: ActorRef, reading: ReducersGroup.ReducerResult,
                       stillWaiting: Set[ActorRef], repliesSoFar: Map[String, ReducersGroup.ReducerResult]): Unit = {
    context.unwatch(reducerActor)
    val reducerId = actorToReducerId(reducerActor)
    val newStillWaiting = stillWaiting - reducerActor
    val newRepliesSoFar = repliesSoFar + (reducerId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! ReducersGroup.RespondAllReduceResults(request, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }

}
