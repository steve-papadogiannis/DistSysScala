import AndroidServer.CalculateDirections
import Main.CreateInfrastracture
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object AndroidServer {
  def props: Props = Props(new  AndroidServer)
  final case class CalculateDirections(requestId: Long, startLat: Double, startLong: Double, endLat: Double, endLong: Double)
}

class AndroidServer extends Actor with ActorLogging {
  var master: ActorRef = _
  override def preStart(): Unit = log.info("Android Server started")
  override def postStop(): Unit = log.info("Android Server stopped")
  override def receive: Receive = {
    case CreateInfrastracture =>
      master = context.actorOf(Master.props)
      master ! CreateInfrastracture
    case request @ CalculateDirections =>
      master forward request
  }


}
