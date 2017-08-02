import MappersGroup.{ReplyMapperList, RequestAllMapResults, RequestMapperList}
import Master.RequestTrackMapper
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

object MappersGroup {
  def props: Props = Props(new MappersGroup)
  final case class RequestMapperList(requestId: Long)
  final case class ReplyMapperList(requestId: Long, ids: Set[String])
  sealed trait MapperResult
  final case class Result(value: Double) extends MapperResult
  case object ResultNotAvailable extends MapperResult
  case object MapperNotAvailable extends MapperResult
  case object MapperTimedOut extends MapperResult
  final case class RequestAllMapResults(requestId: Long)
  final case class RespondAllMapResults(requestId: Long, results: Map[String, MapperResult])
}

class MappersGroup extends Actor with ActorLogging {
  var mapperIdToActor = Map.empty[String, ActorRef]
  var actorToMapperId = Map.empty[ActorRef, String]
  var nextCollectionId = 0L
  override def preStart(): Unit = log.info("MappersGroup started")
  override def postStop(): Unit = log.info("MappersGroup stopped")
  override def receive: Receive = {
    case request @ RequestTrackMapper(mapperId) =>
      mapperIdToActor.get(mapperId) match {
        case Some(mapperActor) =>
          mapperActor forward request
        case None =>
          log.info("Creating mapper actor for {}", mapperId)
          val mapperActor = context.actorOf(MapWorker.props(mapperId), s"device-$mapperId")
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
    case RequestAllMapResults(requestId) =>
      context.actorOf(MappersGroupQuery.props(
        actorToMapperId = actorToMapperId,
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))
  }
}