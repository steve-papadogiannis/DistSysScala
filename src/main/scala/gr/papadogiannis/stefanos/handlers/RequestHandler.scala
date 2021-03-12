package gr.papadogiannis.stefanos.handlers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.Main.supervisor
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages.{CalculateDirections, FinalResponse, Handle}
import gr.papadogiannis.stefanos.models.LatLng

object RequestHandler {
  def props(): Props = Props(new RequestHandler())
}

class RequestHandler() extends Actor with ActorLogging {

  var complete: List[LatLng] => Unit = _

  var requester: ActorRef = _

  override def preStart(): Unit = log.info("RequestHandler actor started")

  override def postStop(): Unit = log.info("RequestHandler actor stopped")

  override def receive: Receive = {
    case message@Handle(requestId, geoPointPair, f) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      requester = sender()
      complete = f
      supervisor ! CalculateDirections(requestId, geoPointPair)
    case message@FinalResponse(_, results) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      results.map(complete)
  }

}