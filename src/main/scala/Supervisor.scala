import akka.actor.{Actor, ActorLogging, Props}

object Supervisor {
  def props(): Props = Props(new Supervisor)
}

class Supervisor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("Application started")
  override def postStop(): Unit = log.info("Application stopped")
  override def receive: Receive= {
    case CreateInfrastracture =>
      val androidServer = context.actorOf(AndroidServer.props)
      androidServer ! CreateInfrastracture
  }
}
