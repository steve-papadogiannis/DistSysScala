package gr.papadogiannis.stefanos.integrations

import akka.actor.{Actor, ActorLogging, Props}
import com.google.maps.{DirectionsApi, GeoApiContext}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages.{GetDirections, GoogleAPIResponse}
import gr.papadogiannis.stefanos.models._

object GoogleDirectionsAPIActor {
  def props(): Props = Props(new GoogleDirectionsAPIActor)
}

class GoogleDirectionsAPIActor extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("GoogleDirectionsAPIActor started")

  override def postStop(): Unit = log.info("GoogleDirectionsAPIActor stopped")

  override def receive: Receive = {
    case message@GetDirections(calculateReduction) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val geoPointPair = calculateReduction.request.geoPointPair
      val geoApiContext = new GeoApiContext
      val apiKey = System.getenv("API_KEY")
      geoApiContext.setApiKey(apiKey)
      val maybeResult = try {
        val googleDirectionsResult = DirectionsApi
          .newRequest(geoApiContext)
          .origin(
            new com.google.maps.model.LatLng(
              geoPointPair.startGeoPoint.latitude,
              geoPointPair.startGeoPoint.longitude))
          .destination(
            new com.google.maps.model.LatLng(
              geoPointPair.endGeoPoint.latitude,
              geoPointPair.endGeoPoint.longitude)).await
        val directionsResult = convert(googleDirectionsResult)
        Some(directionsResult)
      } catch {
        case exception: Exception =>
          log.error(exception.toString)
          None
      }
      sender() ! GoogleAPIResponse(calculateReduction, maybeResult)
  }

  def convert(googleDirectionsResult: com.google.maps.model.DirectionsResult): DirectionsResult = {
    val routes = googleDirectionsResult.routes.toStream.map(route => {
      val legs = route.legs.toStream.map(leg => {
        val steps = leg.steps.toStream.map(step => {
          DirectionsStep(
            LatLng(step.startLocation.lat, step.startLocation.lng),
            LatLng(step.endLocation.lat, step.endLocation.lng),
            EncodedPolyline(step.polyline.getEncodedPath))
        }).toList
        DirectionsLeg(steps, Duration(leg.duration.inSeconds))
      }).toList
      DirectionsRoute(legs)
    }).toList
    DirectionsResult(routes)
  }


}
