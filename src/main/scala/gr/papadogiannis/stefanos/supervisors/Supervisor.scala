package gr.papadogiannis.stefanos.supervisors

import gr.papadogiannis.stefanos.models.CalculateDirections
import gr.papadogiannis.stefanos.Main.CreateInfrastructure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.servers.Server

object Supervisor {
  def props(): Props = Props(new Supervisor)
}

class Supervisor extends Actor with ActorLogging {

  val serverName = "server"

  var server: ActorRef = _

  override def preStart(): Unit = log.info("Supervisor started")

  override def postStop(): Unit = log.info("Supervisor stopped")

  override def receive: Receive = {
    case CreateInfrastructure =>
      server = context.actorOf(Server.props(), serverName)
      server ! CreateInfrastructure
    case request@CalculateDirections(_, _, _, _, _) =>
      server forward request
  }

}
