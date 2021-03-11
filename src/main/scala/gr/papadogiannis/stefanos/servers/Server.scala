package gr.papadogiannis.stefanos.servers

import gr.papadogiannis.stefanos.constants.ApplicationConstants.{MASTER_NAME, RECEIVED_MESSAGE_PATTERN}
import gr.papadogiannis.stefanos.messages.{CalculateDirections, CreateInfrastructure}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.masters.Master

object Server {
  def props(): Props = Props(new Server)
}

class Server extends Actor with ActorLogging {

  var master: ActorRef = _

  override def preStart(): Unit = log.info("Server started")

  override def postStop(): Unit = log.info("Server stopped")

  override def receive: Receive = {
    case CreateInfrastructure =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(CreateInfrastructure))
      master = context.actorOf(Master.props(), MASTER_NAME)
      master ! CreateInfrastructure
    case request@CalculateDirections(_, _) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      master forward request
  }

}
