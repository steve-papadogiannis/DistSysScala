import akka.actor.Props

object MapWorkerImpl {
  def props(a: String): Props = Props(new MapWorkerImpl(a))
}

class MapWorkerImpl(name: String) extends MapWorker {
  override def receive: Receive = ???
}