package gr.papadogiannis.stefanos.mappers

import gr.papadogiannis.stefanos.models.{CalculateDirections, ReplyMapperList, RequestMapperList, RequestTrackMapper}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

object MappersGroup {
  def props(masterActorRef: ActorRef): Props = Props(new MappersGroup(masterActorRef))
}

class MappersGroup(masterActorRef: ActorRef) extends Actor with ActorLogging {

  var mapperIdToActor = Map.empty[String, ActorRef]

  var actorToMapperId = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("MappersGroup started")

  override def postStop(): Unit = log.info("MappersGroup stopped")

  override def receive: Receive = {
    case request@RequestTrackMapper(mapperId) =>
      mapperIdToActor.get(mapperId) match {
        case Some(mapperActor) =>
          mapperActor forward request
        case None =>
          log.info("Creating mapper actor [{}]", mapperId)
          val mapperActor = context.actorOf(
            MapWorker.props(
              mapperId),
            s"$mapperId")
          mapperIdToActor += mapperId -> mapperActor
          actorToMapperId += mapperActor -> mapperId
          mapperActor forward request
      }
    case Terminated(mapperActor) =>
      val mapperId = actorToMapperId(mapperActor)
      log.info("Mapper actor {} has been terminated", mapperId)
      actorToMapperId -= mapperActor
      mapperIdToActor -= mapperId
    case RequestMapperList(requestId) =>
      sender() ! ReplyMapperList(requestId, mapperIdToActor.keySet)
    case request@CalculateDirections(requestId, _, _, _, _) =>
      context.actorOf(
        MappersGroupQuery.props(
          actorToMapperId,
          request,
          masterActorRef,
          5.minutes), s"mappers-group-query-$requestId")
  }

}