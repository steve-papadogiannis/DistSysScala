import MappersGroup.{ReplyMapperList, RequestAllMapResults, RequestMapperList}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object ReducersGroup {

  def props: Props = Props(new ReducersGroup)

}

class ReducersGroup extends Actor with ActorLogging {
   var reducerIdToActor = Map.empty[String, ActorRef]
  var actorToReducerId = Map.empty[ActorRef, String]
  var nextCollectionId = 0L
  override def preStart(): Unit = log.info("MappersGroup started")
  override def postStop(): Unit = log.info("MappersGroup stopped")
  override def receive: Receive = {
    case request @ RequestTrackReducer(reducerId) =>
      reducerIdToActor.get(reducerId) match {
        case Some(reducerActor) =>
          reducerActor forward request
        case None =>
          log.info("Creating mapper actor for {}", reducerId)
          val mapperActor = context.actorOf(ReduceWorkerImpl.props(reducerId), s"device-$reducerId")
          reducerIdToActor += reducerId -> mapperActor
          actorToReducerId += mapperActor -> reducerId
          mapperActor forward request
      }
    case Terminated(mapperActor) =>
      val mapperId = actorToReducerId(mapperActor)
      log.info("Device actor for {} has been terminated", mapperId)
      actorToReducerId -= mapperActor
      reducerIdToActor -= mapperId
    case RequestMapperList(requestId) =>
      sender() ! ReplyMapperList(requestId, reducerIdToActor.keySet)
    case RequestAllMapResults(requestId) =>
      context.actorOf(MapperGroupQuery.props(
        actorToReducerId = actorToReducerId,
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))
  }
}