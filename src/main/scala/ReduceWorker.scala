import akka.actor.{Actor, ActorLogging, Props}

object ReduceWorker {
  def props(reducerId: String): Props = Props(new ReduceWorker(reducerId))
}

class ReduceWorker(name: String) extends Actor with ActorLogging {
  override def receive: Receive = ???
}
