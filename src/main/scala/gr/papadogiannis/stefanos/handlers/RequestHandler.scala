package gr.papadogiannis.stefanos.handlers

import gr.papadogiannis.stefanos.models.{CalculateDirections, FinalResponse, Handle}
import gr.papadogiannis.stefanos.Main.supervisor
import com.google.maps.model.DirectionsResult
import akka.actor.{Actor, ActorRef, Props}

object RequestHandler {
  def props(): Props = Props(new RequestHandler())
}

class RequestHandler() extends Actor {

  var complete: DirectionsResult => Unit = _

  var requester: ActorRef = _

  override def receive: Receive = {
    case Handle(requestId, startLat, startLong, endLat, endLong, f) =>
      requester = sender()
      complete = f
      supervisor ! CalculateDirections(requestId, startLat, startLong, endLat, endLong)
    case FinalResponse(_, results) =>
      complete(results.orNull)
  }

}