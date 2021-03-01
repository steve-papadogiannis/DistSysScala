package gr.papadogiannis.stefanos.mappers

import gr.papadogiannis.stefanos.models.{CalculateDirections, MapperRegistered, ReplyMapperList, RequestMapperList, RequestTrackMapper}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

object MappersGroup {
  def props(masterActorRef: ActorRef): Props = Props(new MappersGroup(masterActorRef))
}

class MappersGroup(masterActorRef: ActorRef) extends Actor with ActorLogging {

  var mapperNameToMapperActorRef = Map.empty[String, ActorRef]

  var mapperActorRefToMapperName = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("MappersGroup started")

  override def postStop(): Unit = log.info("MappersGroup stopped")

  override def receive: Receive = {
    case message@RequestTrackMapper(mapperName) =>
      val mapperActor = mapperNameToMapperActorRef.get(mapperName) match {
        case Some(mapperActor) => mapperActor
        case None =>
          log.info("Creating mapper actor [{}]", mapperName)
          val mapperActor = context.actorOf(MapperWorker.props(mapperName), s"$mapperName")
          mapperActor
      }
      mapperActor ! message
    case Terminated(mapperActor) =>
      val mapperName = mapperActorRefToMapperName(mapperActor)
      log.info("Mapper actor {} has been terminated", mapperName)
      mapperActorRefToMapperName -= mapperActor
      mapperNameToMapperActorRef -= mapperName
    case RequestMapperList(requestId) =>
      sender() ! ReplyMapperList(requestId, mapperNameToMapperActorRef.keySet)
    case request@CalculateDirections(requestId, _, _, _, _) =>
      context.actorOf(MappersGroupQuery.props(mapperActorRefToMapperName, request, masterActorRef, 5.minutes), s"mappers-group-query-$requestId")
    case MapperRegistered(mapperName) =>
      log.info("Registering Mapper Actor [{}]", mapperName)
      mapperNameToMapperActorRef += mapperName -> sender()
      mapperActorRefToMapperName += sender() -> mapperName
  }

}