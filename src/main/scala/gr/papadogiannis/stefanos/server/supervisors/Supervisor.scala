package gr.papadogiannis.stefanos.server.supervisors

import gr.papadogiannis.stefanos.server.servers.Server.CalculateDirections
import gr.papadogiannis.stefanos.server.Main.CreateInfrastracture
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.server.servers.Server

object Supervisor {
  def props(): Props = Props(new Supervisor)
}

class Supervisor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Application started")
  override def postStop(): Unit = log.info("Application stopped")
  var androidServer: ActorRef = _
  override def receive: Receive= {
    case CreateInfrastracture =>
      androidServer = context.actorOf(Server.props)
      androidServer ! CreateInfrastracture
    case request @ CalculateDirections(_,_,_,_,_) =>
      androidServer forward request
  }
}
