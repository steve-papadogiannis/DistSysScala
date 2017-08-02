import ReducersGroupQuery.CollectionTimeout
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}

import scala.concurrent.duration.FiniteDuration

object ReducersGroupQuery {
  case object CollectionTimeout
  def props(actorToDeviceId: Map[ActorRef, String], requestId: Long,
            requester: ActorRef, timeout: FiniteDuration): Props = {
    Props(new ReducersGroupQuery(actorToDeviceId, requestId, requester, timeout))
  }
}

class ReducersGroupQuery(actorToDeviceId: Map[ActorRef, String], requestId: Long,
                         requester: ActorRef, timeout: FiniteDuration) extends Actor with ActorLogging {
  import context.dispatcher
  val queryTimeoutTimer: Cancellable = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)
  override def preStart(): Unit = {
    actorToDeviceId.keysIterator.foreach { deviceActor =>
      context.watch(deviceActor)
      deviceActor ! Device.ReadTemperature(0)
    }
  }
  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }
  override def receive: Receive = waitingForReplies(Map.empty, actorToDeviceId.keySet)
  def waitingForReplies(repliesSoFar: Map[String, ReducersGroup.TemperatureReading],
                        stillWaiting: Set[ActorRef]): Receive = {
    case Device.RespondTemperature(0, valueOption) =>
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) => ReducersGroup.Temperature(value)
        case None        => ReducersGroup.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)
    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, ReducersGroup.DeviceNotAvailable, stillWaiting, repliesSoFar)
    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { deviceActor =>
          val deviceId = actorToDeviceId(deviceActor)
          deviceId -> ReducersGroup.DeviceTimedOut
        }
      requester ! ReducersGroup.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }
  def receivedResponse(deviceActor: ActorRef, reading: ReducersGroup.TemperatureReading,
                       stillWaiting: Set[ActorRef], repliesSoFar: Map[String, ReducersGroup.TemperatureReading]): Unit = {
    context.unwatch(deviceActor)
    val deviceId = actorToDeviceId(deviceActor)
    val newStillWaiting = stillWaiting - deviceActor

    val newRepliesSoFar = repliesSoFar + (deviceId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! ReducersGroup.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }
}
