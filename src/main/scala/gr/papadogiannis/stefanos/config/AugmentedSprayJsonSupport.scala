package gr.papadogiannis.stefanos.config

import gr.papadogiannis.stefanos.models.{DirectionsLeg, DirectionsResult, DirectionsRoute, DirectionsStep, Duration, EncodedPolyline, LatLng}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import gr.papadogiannis.stefanos.messages.{Incoming, Outgoing}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

// The order of the implicits matter, nested to outer
trait AugmentedSprayJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val encodedPolyline: RootJsonFormat[EncodedPolyline] = jsonFormat1(EncodedPolyline)
  implicit val latLng: RootJsonFormat[LatLng] = jsonFormat2(LatLng)
  implicit val directionsStep: RootJsonFormat[DirectionsStep] = jsonFormat3(DirectionsStep)
  implicit val duration: RootJsonFormat[Duration] = jsonFormat1(Duration)
  implicit val directionsLeg: RootJsonFormat[DirectionsLeg] = jsonFormat2(DirectionsLeg)
  implicit val directionsRoute: RootJsonFormat[DirectionsRoute] = jsonFormat1(DirectionsRoute)
  implicit val directionsResult: RootJsonFormat[DirectionsResult] = jsonFormat1(DirectionsResult)
  implicit val incoming: RootJsonFormat[Incoming] = jsonFormat2(Incoming)
  implicit val outgoing: RootJsonFormat[Outgoing] = jsonFormat2(Outgoing)
}
