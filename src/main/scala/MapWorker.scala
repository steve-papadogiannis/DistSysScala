import akka.actor.{Actor, ActorLogging, Props}

object MapWorker {
  def props(a: String): Props = Props(new MapWorker(a))

  case class ReadMapResults(i: Int)

  case class RespondMapResults(i: Int, valueOption: Any)

}

class MapWorker(name: String) extends Actor with ActorLogging {
  override def receive: Receive = ???
}