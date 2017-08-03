import Main.{RequestHandler, counter, system}
import akka.actor.Props
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.duration._

final case class Incoming(name: String, coords: List[Double])

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val incoming: RootJsonFormat[Incoming] = jsonFormat2(Incoming)
}

trait SimpleRoutes extends JsonSupport {
  lazy val simpleRoutes: Route =
    path("getDirections") {
      post {
        entity(as[Incoming]) { entity =>
          implicit val askTimeout: Timeout = 5.minutes // and a timeout
          val actor = system.actorOf(Props[RequestHandler])
          val requestId = counter
          counter += 1
          onSuccess((actor ? RequestHandler.Handle(requestId, 0, 0, 0, 0)).mapTo[RequestHandler.Result]) { result =>
            complete(result.data)
          }

        }
      }
    }
}
