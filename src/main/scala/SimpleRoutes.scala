import AndroidServer.CalculateDirections
import Main.{counter, supervisor, system}
import Master.FinalResponse
import RequestHandler.Handle
import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import com.google.maps.model.DirectionsResult
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import scala.concurrent.duration._

final case class Incoming(name: String, coords: List[Double])
final case class Outgoing(name: String, directionsResult: DirectionsResult)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val incoming: RootJsonFormat[Incoming] = jsonFormat2(Incoming)
  implicit val outgoing: RootJsonFormat[Outgoing] = jsonFormat2(Outgoing)
}

object RequestHandler {
  def props(requestId: Long): Props = Props(new RequestHandler(requestId))
  case class Handle(requestId: Long, startLat: Double, startLong: Double, endLat: Double, endLong: Double, complete: DirectionsResult => Unit)
  case class Result(data: DirectionsResult)
}

class RequestHandler(requestId: Long) extends Actor {
  var requester: ActorRef = _
  var complete: DirectionsResult => Unit = _
  override def receive: Receive = {
    case Handle(_, _, _, _, _, f) =>
      requester = sender()
      complete = f
      supervisor ! CalculateDirections
    case FinalResponse(request, results) =>
      complete(results)
  }
}

trait SimpleRoutes extends JsonSupport {
  lazy val simpleRoutes: Route =
    path("getDirections") {
      post {
        entity(as[Incoming]) { entity =>
          val requestId = counter
          implicit val askTimeout: Timeout = 5.minutes // and a timeout
          counter += 1
          completeWith(implicitly[ToResponseMarshaller[DirectionsResult]]) { f =>
            system.actorOf(RequestHandler.props(requestId), "request-" + requestId) ! RequestHandler.Handle(requestId, entity.coords.head,entity.coords(1), entity.coords(2), entity.coords(3), f)
          }
        }
      }
    }
}
