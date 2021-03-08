package gr.papadogiannis.stefanos.routes

import gr.papadogiannis.stefanos.config.AugmentedSprayJsonSupport
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.PathDirectives.path
import gr.papadogiannis.stefanos.messages.{Handle, Incoming}
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPoint, GeoPointPair}
import gr.papadogiannis.stefanos.handlers.RequestHandler
import gr.papadogiannis.stefanos.Main.{counter, system}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.util.Timeout

import scala.concurrent.duration._

trait SimpleRoutes extends AugmentedSprayJsonSupport {

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case exception: Exception =>
        println(exception.toString)
        complete(HttpResponse(InternalServerError))
    }

  lazy val simpleRoutes: Route = {
    handleExceptions(myExceptionHandler) {
      path("getDirections") {
        post {
          entity(as[Incoming]) { entity =>
            // TODO: validate number of coordinates
            val requestId = counter
            implicit val askTimeout: Timeout = 5.minutes // and a timeout
            counter += 1
            completeWith(implicitly[ToResponseMarshaller[DirectionsResult]]) { f =>
              system.actorOf(RequestHandler.props(), "request-" + requestId) ! Handle(requestId,
                GeoPointPair(
                  GeoPoint(entity.coords.head, entity.coords(1)),
                  GeoPoint(entity.coords(2), entity.coords(3))), f)
            }
          }
        }
      }
    }
  }

}
