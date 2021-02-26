package gr.papadogiannis.stefanos.server

import gr.papadogiannis.stefanos.server.routes.{BaseRoutes, SimpleRoutes}
import gr.papadogiannis.stefanos.server.supervisors.Supervisor
import akka.http.scaladsl.server.{Directives, Route}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main
  extends Directives
    with SimpleRoutes {

  var counter: Long = 0L

  object CreateInfrastructure

  def main(args: Array[String]): Unit = {

    system = ActorSystem("DirectionsResultMapReduceSystem")

    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8383)

    supervisor = system.actorOf(Supervisor.props(), "supervisor")

    supervisor ! CreateInfrastructure

    println(s"Server online at http://localhost:8383/\nPress RETURN to stop...")

    StdIn.readLine()

    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

  }

  val routes: Route = BaseRoutes.baseRoutes ~ simpleRoutes

  implicit var system: ActorSystem = _

  var supervisor: ActorRef = _

}
