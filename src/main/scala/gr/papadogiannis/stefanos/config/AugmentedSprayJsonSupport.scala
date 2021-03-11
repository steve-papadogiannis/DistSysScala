package gr.papadogiannis.stefanos.config

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import gr.papadogiannis.stefanos.models._

// The order of the implicits matter, nested to outer
trait AugmentedSprayJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val encodedPolyline: RootJsonFormat[EncodedPolyline] = jsonFormat1(EncodedPolyline)
  implicit val latLng: RootJsonFormat[LatLng] = jsonFormat2(LatLng)
  implicit val directionsStep: RootJsonFormat[DirectionsStep] = jsonFormat1(DirectionsStep)
  implicit val duration: RootJsonFormat[Duration] = jsonFormat1(Duration)
  implicit val directionsLeg: RootJsonFormat[DirectionsLeg] = jsonFormat4(DirectionsLeg)
  implicit val directionsRoute: RootJsonFormat[DirectionsRoute] = jsonFormat1(DirectionsRoute)
  implicit val directionsResult: RootJsonFormat[DirectionsResult] = jsonFormat1(DirectionsResult)
  implicit val geoPoint: RootJsonFormat[GeoPoint] = jsonFormat2(GeoPoint)
  implicit val geoPointPair: RootJsonFormat[GeoPointPair] = jsonFormat2(GeoPointPair)
}
