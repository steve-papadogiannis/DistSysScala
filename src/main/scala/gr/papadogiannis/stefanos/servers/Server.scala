package gr.papadogiannis.stefanos.servers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.Main.CreateInfrastructure
import gr.papadogiannis.stefanos.masters.Master
import gr.papadogiannis.stefanos.servers.Server.CalculateDirections

object Server {

  def props: Props = Props(new  Server)

  final case class CalculateDirections(requestId: Long, startLat: Double,
                                       startLong: Double, endLat: Double,
                                       endLong: Double)

}

class Server
  extends Actor
    with ActorLogging {

  var master: ActorRef = _

  override def preStart(): Unit = log.info("Server started")

  override def postStop(): Unit = log.info("Server stopped")

  override def receive: Receive = {
    case CreateInfrastructure =>
      master = context.actorOf(Master.props)
      master ! CreateInfrastructure
    case request @ CalculateDirections =>
      master forward request
  }

}
