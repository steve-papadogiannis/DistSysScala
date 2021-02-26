package gr.papadogiannis.stefanos.server.servers

import gr.papadogiannis.stefanos.server.servers.Server.CalculateDirections
import gr.papadogiannis.stefanos.server.Main.CreateInfrastructure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.server.masters.Master

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
