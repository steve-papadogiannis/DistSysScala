package gr.papadogiannis.stefanos.mappers

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages.{CalculateDirections, MapperRegistered, ReplyMapperList, RequestMapperList, RequestTrackMapper}

import scala.concurrent.duration._

object MappersGroup {
  def props(masterActorRef: ActorRef, mongoActorRef: ActorRef): Props =
    Props(new MappersGroup(masterActorRef, mongoActorRef))
}

class MappersGroup(masterActorRef: ActorRef, mongoActorRef: ActorRef)
  extends Actor with ActorLogging {

  var mapperNameToMapperActorRef = Map.empty[String, ActorRef]

  var mapperActorRefToMapperName = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("MappersGroup started")

  override def postStop(): Unit = log.info("MappersGroup stopped")

  override def receive: Receive = {
    case message@RequestTrackMapper(mapperName) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val mapperActor = mapperNameToMapperActorRef.get(mapperName) match {
        case Some(mapperActor) => mapperActor
        case None =>
          log.info("Creating mapper actor [{}]", mapperName)
          val mapperActor = context.actorOf(MapperWorker.props(mapperName, mongoActorRef), s"$mapperName")
          mapperActor
      }
      mapperActor ! message
    case message@Terminated(mapperActor) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val mapperName = mapperActorRefToMapperName(mapperActor)
      log.info("Mapper actor {} has been terminated", mapperName)
      mapperActorRefToMapperName -= mapperActor
      mapperNameToMapperActorRef -= mapperName
    case message@RequestMapperList(requestId) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      sender() ! ReplyMapperList(requestId, mapperNameToMapperActorRef.keySet)
    case request@CalculateDirections(requestId, _) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      context.actorOf(MappersGroupQuery.props(mapperActorRefToMapperName, request, masterActorRef, 5.minutes), s"mappers-group-query-$requestId")
    case message@MapperRegistered(mapperName) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      log.info("Registering Mapper Actor [{}]", mapperName)
      mapperNameToMapperActorRef += mapperName -> sender()
      mapperActorRefToMapperName += sender() -> mapperName
  }

}