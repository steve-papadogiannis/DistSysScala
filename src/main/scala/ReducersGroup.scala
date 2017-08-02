import Master.RequestTrackReducer
import ReducersGroup.{ReplyReducerList, RequestAllReduceResults, RequestReducerList}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration._

object ReducersGroup {
  def props(mappersGroupActorRef: ActorRef, masterActorRef: ActorRef): Props = Props(new ReducersGroup)
  final case class RequestReducerList(requestId: Long)
  final case class ReplyReducerList(requestId: Long, ids: Set[String])
  sealed trait ReducerResult
  final case class Result(value: Double) extends ReducerResult
  case object ResultNotAvailable extends ReducerResult
  case object ReducerNotAvailable extends ReducerResult
  case object ReducerTimedOut extends ReducerResult
  final case class RequestAllReduceResults(requestId: Long)
  final case class RespondAllReduceResults(requestId: Long, results: Map[String, ReducerResult])

  case class MapResult(value: Any)

}

class ReducersGroup extends Actor with ActorLogging {
  var reducerIdToActor = Map.empty[String, ActorRef]
  var actorToReducerId = Map.empty[ActorRef, String]
  var nextCollectionId = 0L
  override def preStart(): Unit = log.info("ReducersGroup started")
  override def postStop(): Unit = log.info("ReducersGroup stopped")
  override def receive: Receive = {
    case request @ RequestTrackReducer(reducerId) =>
      reducerIdToActor.get(reducerId) match {
        case Some(reducerActor) =>
          reducerActor forward request
        case None =>
          log.info("Creating mapper actor for {}", reducerId)
          val reducerActor = context.actorOf(ReduceWorker.props(reducerId), s"device-$reducerId")
          reducerIdToActor += reducerId -> reducerActor
          actorToReducerId += reducerActor -> reducerId
          reducerActor forward request
      }
    case Terminated(reducerActor) =>
      val reducerId = actorToReducerId(reducerActor)
      log.info("Device actor for {} has been terminated", reducerId)
      actorToReducerId -= reducerActor
      reducerIdToActor -= reducerId
    case RequestReducerList(requestId) =>
      sender() ! ReplyReducerList(requestId, reducerIdToActor.keySet)
    case RequestAllReduceResults(requestId) =>
      context.actorOf(ReducersGroupQuery.props(
        actorToReducerId = actorToReducerId,
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))
  }
}