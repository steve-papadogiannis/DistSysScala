package gr.papadogiannis.stefanos.config

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import gr.papadogiannis.stefanos.models.{Incoming, Outgoing}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import com.google.maps.model.DirectionsResult

trait AugmentedSprayJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val directionsResult: RootJsonFormat[DirectionsResult] = jsonFormat0(() => new DirectionsResult())
  implicit val incoming: RootJsonFormat[Incoming] = jsonFormat2(Incoming)
  implicit val outgoing: RootJsonFormat[Outgoing] = jsonFormat2(Outgoing)
}
