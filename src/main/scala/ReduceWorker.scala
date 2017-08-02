import akka.actor.{Actor, ActorLogging, Props}

object ReduceWorker {
  def props(reducerId: String): Props = Props(new ReduceWorker(reducerId))

  case class ReadReduceResult(i: Int)

  case class RespondeReduceResult(i: Int, valueOption: Any)

}

class ReduceWorker(name: String) extends Actor with ActorLogging {
  override def receive: Receive = ???
}
