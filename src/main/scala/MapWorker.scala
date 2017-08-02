import akka.actor.Props

object MapWorker {
  def props(a: String): Props = Props(new MapWorker(a))
}

class MapWorker(name: String) extends MapWorker {
  override def receive: Receive = ???
}