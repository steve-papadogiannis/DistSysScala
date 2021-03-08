package gr.papadogiannis.stefanos

import gr.papadogiannis.stefanos.routes.{BaseRoutes, SimpleRoutes}
import gr.papadogiannis.stefanos.supervisors.Supervisor
import akka.http.scaladsl.server.{Directives, Route}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import gr.papadogiannis.stefanos.messages.CreateInfrastructure

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends Directives with SimpleRoutes {

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
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val bindingFuture = Http().bindAndHandle(routes, hostName, port)

    println(s"Http Server Binding bound at http://$hostName:$port/\nSubmit any input to STOP...")

    StdIn.readLine()

    bindingFuture.flatMap(serverBinding => {
      println(s"Http Server Binding unbound from http://$hostName:$port/")
      serverBinding.unbind()
    }).onComplete(_ => {
      println(s"Actor System $actorSystemName is terminating...")
      system.terminate()
    })

  }

}
