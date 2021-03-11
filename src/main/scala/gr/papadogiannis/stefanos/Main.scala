package gr.papadogiannis.stefanos

import gr.papadogiannis.stefanos.constants.ApplicationConstants.{ACTOR_SYSTEM_NAME, DEFAULT_HOST_NAME, DEFAULT_PORT, SUPERVISOR_NAME}
import gr.papadogiannis.stefanos.routes.{BaseRoutes, SimpleRoutes}
import gr.papadogiannis.stefanos.messages.CreateInfrastructure
import gr.papadogiannis.stefanos.supervisors.Supervisor
import akka.http.scaladsl.server.{Directives, Route}
import akka.actor.{ActorRef, ActorSystem}
import org.slf4j.{Logger, LoggerFactory}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends Directives with SimpleRoutes {

  val routes: Route = BaseRoutes.baseRoutes ~ simpleRoutes

  implicit var system: ActorSystem = _
  var supervisor: ActorRef = _
  var counter: Long = 0L

  val logger: Logger = LoggerFactory.getLogger(Main.getClass)

  def main(args: Array[String]): Unit = {

    val hostName = args.headOption.getOrElse(DEFAULT_HOST_NAME);
    val port = Option(args)
      .filter(args => args.length > 1)
      .map(args => args(1))
      .map(_.toInt)
      .getOrElse(DEFAULT_PORT)

    system = ActorSystem(ACTOR_SYSTEM_NAME)

    supervisor = system.actorOf(Supervisor.props(), SUPERVISOR_NAME)
    supervisor ! CreateInfrastructure

    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val bindingFuture = Http().bindAndHandle(routes, hostName, port)

    logger.info(s"Http Server Binding bound at http://$hostName:$port/\nSubmit any input to STOP...")

    StdIn.readLine()

    bindingFuture.flatMap(serverBinding => {
      logger.info(s"Http Server Binding unbound from http://$hostName:$port/")
      serverBinding.unbind()
    }).onComplete(_ => {
      logger.info(s"Actor System $ACTOR_SYSTEM_NAME is terminating...")
      system.terminate()
    })

  }

}
