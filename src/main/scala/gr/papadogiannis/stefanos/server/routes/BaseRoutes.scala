package gr.papadogiannis.stefanos.server.routes

import akka.http.scaladsl.server.directives.PathDirectives.pathEndOrSingleSlash
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.Route

object BaseRoutes {

  lazy val baseRoutes: Route =
    pathEndOrSingleSlash {
      complete("Server up and running")
    }

}
