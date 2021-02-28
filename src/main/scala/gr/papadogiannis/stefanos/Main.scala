package gr.papadogiannis.stefanos

import gr.papadogiannis.stefanos.routes.{BaseRoutes, SimpleRoutes}
import gr.papadogiannis.stefanos.supervisors.Supervisor
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

  val actorSystemName = "directions-map-reduce-actor-system"
  val supervisorName = "supervisor"

  object CreateInfrastructure

  def main(args: Array[String]): Unit = {

    system = ActorSystem(actorSystemName)

    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val port = args.head.toInt;

    val bindingFuture = Http().bindAndHandle(routes, "localhost", port)
    supervisor = system.actorOf(Supervisor.props(), supervisorName)

    supervisor ! CreateInfrastructure

    println(s"ActorSystem [$actorSystemName] started at http://localhost:$port/\nPress RETURN to stop...")

    StdIn.readLine()

    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

  }

  val routes: Route = BaseRoutes.baseRoutes ~ simpleRoutes

  implicit var system: ActorSystem = _

  var supervisor: ActorRef = _

}
