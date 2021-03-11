package gr.papadogiannis.stefanos.routes

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}
import gr.papadogiannis.stefanos.config.AugmentedSprayJsonSupport
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.directives.PathDirectives.path
import gr.papadogiannis.stefanos.validators.JSONBodyValidator
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import gr.papadogiannis.stefanos.handlers.RequestHandler
import gr.papadogiannis.stefanos.Main.{counter, system}
import gr.papadogiannis.stefanos.messages.Handle
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.HttpResponse
import akka.util.Timeout
import org.slf4j.Logger

import scala.concurrent.duration._

trait SimpleRoutes extends AugmentedSprayJsonSupport with JSONBodyValidator {

  implicit var log: Logger = _

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case exception: Exception =>
        log.error(exception.toString)
        complete(HttpResponse(InternalServerError))
    }

  lazy val simpleRoutes: Route = {
    handleExceptions(exceptionHandler) {
      path("getDirections") {
        post {
          entity(as[GeoPointPair].validate) { entity =>
            val requestId = counter
            implicit val askTimeout: Timeout = 5.minutes // and a timeout
            counter += 1
            completeWith(implicitly[ToResponseMarshaller[DirectionsResult]]) { f =>
              system.actorOf(RequestHandler.props(), "request-" + requestId) ! Handle(requestId, entity, f)
            }
          }
        }
      }
    }
  }

}
