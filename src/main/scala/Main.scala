import AndroidServer.CalculateDirections
import Master.FinalResponse
import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer

import scala.io.StdIn

object Main extends Directives with SimpleRoutes {
  var counter: Long = 0L
  object CreateInfrastracture
  object RequestHandler {
    case class Handle(requestId: Long, startLat: Long, startLong: Long, endLat: Long, endLong: Long)
    case class Result(data: String)
  }
  class RequestHandler extends Actor {
    import RequestHandler._
    var requester: ActorRef = _
    def receive: PartialFunction[Any, Unit] = {
      case Handle =>
        requester = sender()
        supervisor ! CalculateDirections
      case FinalResponse(request, results) =>
        requester ! Result("fadf")
    }
  }

  def main(args: Array[String]): Unit = {
    system = ActorSystem("DirectionsResultMapReduceSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8484)
    supervisor = system.actorOf(Supervisor.props(), "supervisor")
    supervisor ! CreateInfrastracture
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
  val routes: Route = BaseRoutes.baseRoutes ~ simpleRoutes
  implicit var system: ActorSystem = _
  var supervisor: ActorRef = _
}
