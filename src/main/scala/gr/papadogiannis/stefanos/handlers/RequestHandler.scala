package gr.papadogiannis.stefanos.handlers

import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages.{CalculateDirections, FinalResponse, Handle}
import gr.papadogiannis.stefanos.models.DirectionsResult
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.Main.supervisor

object RequestHandler {
  def props(): Props = Props(new RequestHandler())
}

class RequestHandler() extends Actor with ActorLogging {

  var complete: DirectionsResult => Unit = _

  var requester: ActorRef = _

  override def preStart(): Unit = log.info("RequestHandler actor started")

  override def postStop(): Unit = log.info("RequestHandler actor stopped")

  override def receive: Receive = {
    case message@Handle(requestId, startLat, startLong, endLat, endLong, f) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      requester = sender()
      complete = f
      supervisor ! CalculateDirections(requestId, startLat, startLong, endLat, endLong)
    case message@FinalResponse(_, results) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      complete(results.orNull)
  }

}