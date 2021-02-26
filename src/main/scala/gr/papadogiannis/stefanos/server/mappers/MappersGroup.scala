package gr.papadogiannis.stefanos.server.mappers

import gr.papadogiannis.stefanos.server.mappers.MappersGroup.{ReplyMapperList, RequestMapperList}
import gr.papadogiannis.stefanos.server.servers.Server.CalculateDirections
import gr.papadogiannis.stefanos.server.masters.Master.RequestTrackMapper
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import gr.papadogiannis.stefanos.server.models.GeoPointPair
import com.google.maps.model.DirectionsResult

import scala.concurrent.duration._

object MappersGroup {
  def props(reducersGroupActorRef: ActorRef, masterActorRef: ActorRef): Props = Props(new MappersGroup(reducersGroupActorRef, masterActorRef))
  final case class RequestMapperList(requestId: Long)
  final case class ReplyMapperList(requestId: Long, ids: Set[String])
  sealed trait MapperResult
  final case class ConcreteResult(value: List[Map[GeoPointPair, DirectionsResult]]) extends MapperResult
  case object ResultNotAvailable extends MapperResult
  case object MapperNotAvailable extends MapperResult
  case object MapperTimedOut extends MapperResult
  final case class RespondAllMapResults(request: CalculateDirections, results: Map[String, MapperResult])
}

class MappersGroup(reducersGroupActorRef: ActorRef, masterActorRef: ActorRef) extends Actor with ActorLogging {
  var mapperIdToActor = Map.empty[String, ActorRef]
  var actorToMapperId = Map.empty[ActorRef, String]
  override def preStart(): Unit = log.info("MappersGroup started")
  override def postStop(): Unit = log.info("MappersGroup stopped")
  override def receive: Receive = {
    case request @ RequestTrackMapper(mapperId) =>
      mapperIdToActor.get(mapperId) match {
        case Some(mapperActor) =>
          mapperActor forward request
        case None =>
          log.info("Creating mapper actor for {}", mapperId)
          val mapperActor = context.actorOf(MapWorker.props(mapperId, reducersGroupActorRef, masterActorRef), s"device-$mapperId")
          mapperIdToActor += mapperId -> mapperActor
          actorToMapperId += mapperActor -> mapperId
          mapperActor forward request
      }
    case Terminated(mapperActor) =>
      val mapperId = actorToMapperId(mapperActor)
      log.info("Device actor for {} has been terminated", mapperId)
      actorToMapperId -= mapperActor
      mapperIdToActor -= mapperId
    case RequestMapperList(requestId) =>
      sender() ! ReplyMapperList(requestId, mapperIdToActor.keySet)
    case request @ CalculateDirections(_, _, _, _, _) =>
      context.actorOf(MappersGroupQuery.props(actorToMapperId, request, masterActorRef, 5.minutes))
  }
}