package gr.papadogiannis.stefanos.routes

import gr.papadogiannis.stefanos.config.AugmentedSprayJsonSupport
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import gr.papadogiannis.stefanos.models.{Handle, Incoming}
import gr.papadogiannis.stefanos.handlers.RequestHandler
import gr.papadogiannis.stefanos.Main.{counter, system}
import com.google.maps.model.DirectionsResult
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.duration._

trait SimpleRoutes extends AugmentedSprayJsonSupport {

  lazy val simpleRoutes: Route =
    path("getDirections") {
      post {
        entity(as[Incoming]) { entity =>
          val requestId = counter
          implicit val askTimeout: Timeout = 5.minutes // and a timeout
          counter += 1
          completeWith(implicitly[ToResponseMarshaller[DirectionsResult]]) { f =>
            system.actorOf(RequestHandler.props(), "request-" + requestId) ! Handle(requestId, entity.coords.head, entity.coords(1), entity.coords(2), entity.coords(3), f)
          }
        }
      }
    }

}
