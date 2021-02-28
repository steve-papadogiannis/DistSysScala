package gr.papadogiannis.stefanos.mappers

import MappersGroup.{ReplyMapperList, RequestMapperList}
import gr.papadogiannis.stefanos.servers.Server.CalculateDirections
import gr.papadogiannis.stefanos.masters.Master.RequestTrackMapper
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.google.maps.model.DirectionsResult
import gr.papadogiannis.stefanos.models.GeoPointPair

import scala.concurrent.duration._

object MappersGroup {

  def props(reducersGroupActorRef: ActorRef, masterActorRef: ActorRef): Props =
    Props(new MappersGroup(reducersGroupActorRef, masterActorRef))

  final case class RequestMapperList(requestId: Long)

  final case class ReplyMapperList(requestId: Long, ids: Set[String])

  sealed trait MapperResult

  final case class ConcreteResult(value: List[Map[GeoPointPair, DirectionsResult]]) extends MapperResult

  case object ResultNotAvailable extends MapperResult

  case object MapperNotAvailable extends MapperResult

  case object MapperTimedOut extends MapperResult

  final case class RespondAllMapResults(request: CalculateDirections, results: Map[String, MapperResult])

}

class MappersGroup(reducersGroupActorRef: ActorRef, masterActorRef: ActorRef)
  extends Actor
    with ActorLogging {

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
              mapperId,
              reducersGroupActorRef,
              masterActorRef),
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