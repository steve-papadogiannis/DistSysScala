import akka.actor.{Actor, ActorLogging, Props}

object MapWorker {
  def props(a: String): Props = Props(new MapWorker(a))
}

class MapWorker(name: String) extends Actor with ActorLogging {
  override def receive: Receive = ???
}