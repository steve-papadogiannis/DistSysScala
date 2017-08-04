import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorMaterializer

import scala.io.StdIn

object Main extends Directives with SimpleRoutes {
  var counter: Long = 0L
  object CreateInfrastracture

  def main(args: Array[String]): Unit = {
    system = ActorSystem("DirectionsResultMapReduceSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8383)
    supervisor = system.actorOf(Supervisor.props(), "supervisor")
    supervisor ! CreateInfrastracture
    println(s"Server online at http://localhost:8383/\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())
  }
  val routes: Route = BaseRoutes.baseRoutes ~ simpleRoutes
  implicit var system: ActorSystem = _
  var supervisor: ActorRef = _
}
