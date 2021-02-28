package gr.papadogiannis.stefanos.mappers

import gr.papadogiannis.stefanos.models.{CalculateDirections, ConcreteMapperResult, MapperNotAvailable, MapperResult, MapperTimedOut, RespondAllMapResults, RespondMapResults}
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}
import MappersGroupQuery.CollectionTimeout

import scala.concurrent.duration.FiniteDuration

object MappersGroupQuery {
  case object CollectionTimeout

  def props(actorToMapperId: Map[ActorRef, String],
            request: CalculateDirections,
            requester: ActorRef,
            timeout: FiniteDuration): Props =
    Props(new MappersGroupQuery(actorToMapperId, request, requester, timeout))
}

class MappersGroupQuery(actorToMapperId: Map[ActorRef, String],
                        request: CalculateDirections,
                        requester: ActorRef,
                        timeout: FiniteDuration) extends Actor with ActorLogging {

  import context.dispatcher

  val queryTimeoutTimer: Cancellable =
    context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToMapperId.keysIterator.foreach { mapperActor =>
      context.watch(mapperActor)
      mapperActor ! request
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }

  override def receive: Receive = waitingForReplies(Map.empty, actorToMapperId.keySet)

  def waitingForReplies(repliesSoFar: Map[String, MapperResult],
                        stillWaiting: Set[ActorRef]): Receive = {
    case RespondMapResults(_, valueOption) =>
      val mapperActor = sender()
      val reading = ConcreteMapperResult(valueOption)
      receivedResponse(mapperActor, reading, stillWaiting, repliesSoFar)
    case Terminated(mapperActor) =>
      receivedResponse(mapperActor, MapperNotAvailable, stillWaiting, repliesSoFar)
    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { mapperActor =>
          val mapperId = actorToMapperId(mapperActor)
          mapperId -> MapperTimedOut
        }
      requester ! RespondAllMapResults(request, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(mapperActor: ActorRef,
                       reading: MapperResult,
                       stillWaiting: Set[ActorRef],
                       repliesSoFar: Map[String, MapperResult]): Unit = {
    context.unwatch(mapperActor)
    val deviceId = actorToMapperId(mapperActor)
    val newStillWaiting = stillWaiting - mapperActor
    val newRepliesSoFar = repliesSoFar + (deviceId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! RespondAllMapResults(request, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }

}
