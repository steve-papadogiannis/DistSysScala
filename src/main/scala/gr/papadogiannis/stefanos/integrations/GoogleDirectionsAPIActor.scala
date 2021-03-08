package gr.papadogiannis.stefanos.integrations

import akka.actor.{Actor, ActorLogging, Props}
import com.google.maps.errors.ApiException
import com.google.maps.model.LatLng
import com.google.maps.{DirectionsApi, GeoApiContext}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages.GetDirections

import java.io.IOException

object GoogleDirectionsAPIActor {
  def props(): Props = Props(new GoogleDirectionsAPIActor)
}

class GoogleDirectionsAPIActor extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("GoogleDirectionsAPIActor started")

  override def postStop(): Unit = log.info("GoogleDirectionsAPIActor stopped")

  override def receive: Receive = {
    case message@GetDirections(geoPointPair) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val geoApiContext = new GeoApiContext
      geoApiContext.setApiKey("")
      try {
        val await = DirectionsApi
          .newRequest(geoApiContext)
          .origin(new LatLng(geoPointPair.startGeoPoint.latitude, geoPointPair.startGeoPoint.longitude))
          .destination(new LatLng(geoPointPair.endGeoPoint.latitude, geoPointPair.endGeoPoint.longitude)).await
        sender() ! await
      } catch {
        case exception@(_: ApiException | _: InterruptedException | _: IOException) =>
          log.error(exception.toString)
      }
  }

}
