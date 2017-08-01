import MappersGroup.{ReplyDeviceList, RequestDeviceList}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

object MappersGroup {
  def props: Props = Props(new MappersGroup)
  final case class RequestDeviceList(requestId: Long)
  final case class ReplyDeviceList(requestId: Long, ids: Set[String])
  sealed trait TemperatureReading
  final case class Temperature(value: Double) extends TemperatureReading
  case object TemperatureNotAvailable extends TemperatureReading
  case object DeviceNotAvailable extends TemperatureReading
  case object DeviceTimedOut extends TemperatureReading
  final case class RequestAllTemperatures(requestId: Long)
  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

}

class MappersGroup extends Actor with ActorLogging {
  var mapperIdToActor = Map.empty[String, ActorRef]
  var actorToMapperId = Map.empty[ActorRef, String]
  var nextCollectionId = 0L
  override def preStart(): Unit = log.info("MappersGroup started")
  override def postStop(): Unit = log.info("MappersGroup stopped")
  override def receive: Receive = {
    case request @ RequestTrackMapper(a) =>
      mapperIdToActor.get(a) match {
        case Some(deviceActor) =>
          deviceActor forward request
        case None =>
          log.info("Creating mapper actor for {}", a)
          val mapperActor = context.actorOf(MapWorkerImpl.props(a), s"device-${a}")
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          actorToDeviceId += deviceActor -> trackMsg.deviceId
          deviceActor forward trackMsg
      }
    case Terminated(deviceActor) =>
      val deviceId = actorToDeviceId(deviceActor)
      log.info("Device actor for {} has been terminated", deviceId)
      actorToDeviceId -= deviceActor
      deviceIdToActor -= deviceId
    case RequestDeviceList(requestId) =>
      sender() ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
    case RequestAllTemperatures(requestId) =>
      context.actorOf(DeviceGroupQuery.props(
        actorToDeviceId = actorToDeviceId,
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))
  }
}