package gr.papadogiannis.stefanos

import gr.papadogiannis.stefanos.routes.{BaseRoutes, SimpleRoutes}
import gr.papadogiannis.stefanos.supervisors.Supervisor
import akka.http.scaladsl.server.{Directives, Route}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends Directives with SimpleRoutes {

  object CreateInfrastructure

  val actorSystemName = "directions-map-reduce-actor-system"
  val defaultHostName = "localhost"
  val supervisorName = "supervisor"
  val defaultPort: Int = 8383

  val routes: Route = BaseRoutes.baseRoutes ~ simpleRoutes

  implicit var system: ActorSystem = _
  var supervisor: ActorRef = _
  var counter: Long = 0L

  def main(args: Array[String]): Unit = {

    val hostName = args.headOption.getOrElse(defaultHostName);
    val port = Option(args)
      .filter(args => args.length > 1)
      .map(args => args(1))
      .map(_.toInt)
      .getOrElse(defaultPort)

    system = ActorSystem(actorSystemName)

    supervisor = system.actorOf(Supervisor.props(), supervisorName)
    supervisor ! CreateInfrastructure

    implicit val materializer: ActorMaterializer = ActorMaterializer()
    val bindingFuture = Http().bindAndHandle(routes, hostName, port)

    println(s"ActorSystem [$actorSystemName] started at http://$hostName:$port/\nPress RETURN to stop...")

    StdIn.readLine()

    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    bindingFuture.flatMap(_.unbind()).onComplete(_ => system.terminate())

  }

}
