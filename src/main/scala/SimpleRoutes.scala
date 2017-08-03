import Main.{RequestHandler, system}
import akka.actor.Props
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import scala.concurrent.duration._

trait SimpleRoutes {
  lazy val simpleRoutes: Route =
    path("getDirections") {
      post {
        implicit val askTimeout: Timeout = 5.minutes // and a timeout
        val actor = system.actorOf(Props[RequestHandler])
        onSuccess((actor ? RequestHandler.Handle).mapTo[RequestHandler.Result]) { result =>
          complete(result.data)
        }
      }
    }
}
