import akka.actor.Props

object ReduceWorker {
  def props(reducerId: String): Props = Props(new ReduceWorker(reducerId))

  case class RequestTrackReducer(name: String)
}

class ReduceWorker(name: String) extends ReduceWorker {
  override def receive: Receive = ???
}
