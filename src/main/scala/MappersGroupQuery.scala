import MappersGroupQuery.CollectionTimeout
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

object MappersGroupQuery {
  case object CollectionTimeout
  def props(actorToDeviceId: Map[ActorRef, String], requestId: Long,
    requester: ActorRef, timeout: FiniteDuration): Props = {
      Props(new ReducersGroupQuery(actorToDeviceId, requestId, requester, timeout))
  }
}

class MappersGroupQuery(actorToMapperId: Map[ActorRef, String], requestId: Long,
  requester: ActorRef, timeout: FiniteDuration) extends Actor with ActorLogging {
  import context.dispatcher
  val queryTimeoutTimer: Cancellable = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)
  override def preStart(): Unit = {
    actorToMapperId.keysIterator.foreach { mapperActor =>
      context.watch(mapperActor)
      mapperActor ! MapWorker.ReadMapResults(0)
    }
  }
  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }
  override def receive: Receive = waitingForReplies(Map.empty, actorToMapperId.keySet)
  def waitingForReplies(repliesSoFar: Map[String, MappersGroup.Result],
    stillWaiting: Set[ActorRef]): Receive = {
    case MapWorker.RespondMapResults(0, valueOption) =>
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) => ReducersGroup.Temperature(value)
        case None        => ReducersGroup.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)
    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, MappersGroup.MapperNotAvailable, stillWaiting, repliesSoFar)
    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { mapActor =>
          val mapperId = actorToMapperId(mapActor)
          mapperId -> MappersGroup.MapperTimedOut
        }
      requester ! MappersGroup.RespondAllMapResults(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }
  def receivedResponse(mapperActor: ActorRef, reading: MappersGroup.Result,
    stillWaiting: Set[ActorRef], repliesSoFar: Map[String, ReducersGroup.TemperatureReading]): Unit = {
    context.unwatch(mapperActor)
    val deviceId = actorToMapperId(mapperActor)
    val newStillWaiting = stillWaiting - mapperActor
    val newRepliesSoFar = repliesSoFar + (deviceId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! ReducersGroup.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }
}
