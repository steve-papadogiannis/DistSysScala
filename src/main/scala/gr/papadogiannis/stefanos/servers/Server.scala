package gr.papadogiannis.stefanos.servers

import gr.papadogiannis.stefanos.models.CalculateDirections
import gr.papadogiannis.stefanos.Main.CreateInfrastructure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.masters.Master

object Server {
  def props(): Props = Props(new Server)
}

class Server extends Actor with ActorLogging {

  val masterName = "master"

  var master: ActorRef = _

  override def preStart(): Unit = log.info("Server started")

  override def postStop(): Unit = log.info("Server stopped")

  override def receive: Receive = {
    case CreateInfrastructure =>
      master = context.actorOf(Master.props(), masterName)
      master ! CreateInfrastructure
    case request@CalculateDirections(_, _, _, _, _) =>
      master forward request
  }

}
