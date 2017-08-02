import ReducersGroupQuery.CollectionTimeout
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

object ReducersGroupQuery {
  case object CollectionTimeout
  def props(actorToReducerId: Map[ActorRef, String], requestId: Long,
    requester: ActorRef, timeout: FiniteDuration): Props = Props(new ReducersGroupQuery(actorToReducerId, requestId, requester, timeout))
}

class ReducersGroupQuery(actorToReducerId: Map[ActorRef, String], requestId: Long,
  requester: ActorRef, timeout: FiniteDuration) extends Actor with ActorLogging {
  import context.dispatcher
  val queryTimeoutTimer: Cancellable = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)
  override def preStart(): Unit = {
    actorToReducerId.keysIterator.foreach { reducerActor =>
      context.watch(reducerActor)
      reducerActor ! ReduceWorker.ReadReduceResult(0)
    }
  }
  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }
  override def receive: Receive = waitingForReplies(Map.empty, actorToReducerId.keySet)
  def waitingForReplies(repliesSoFar: Map[String, ReducersGroup.ReducerResult],
    stillWaiting: Set[ActorRef]): Receive = {
    case ReduceWorker.RespondeReduceResult(0, valueOption) =>
      val reducerActor = sender()
      val reading = valueOption match {
        case Some(value) => ReducersGroup.Temperature(value)
        case None        => ReducersGroup.TemperatureNotAvailable
      }
      receivedResponse(reducerActor, reading, stillWaiting, repliesSoFar)
    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, ReducersGroup.ReducerNotAvailable, stillWaiting, repliesSoFar)
    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { deviceActor =>
          val reducerId = actorToReducerId(deviceActor)
          reducerId -> ReducersGroup.ReducerTimedOut
        }
      requester ! ReducersGroup.RespondAllReduceResults(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }
  def receivedResponse(reducerActor: ActorRef, reading: ReducersGroup.ReducerResult,
    stillWaiting: Set[ActorRef], repliesSoFar: Map[String, ReducersGroup.ReducerResult]): Unit = {
    context.unwatch(reducerActor)
    val reducerId = actorToReducerId(reducerActor)
    val newStillWaiting = stillWaiting - reducerActor
    val newRepliesSoFar = repliesSoFar + (reducerId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! ReducersGroup.RespondAllReduceResults(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }
}
