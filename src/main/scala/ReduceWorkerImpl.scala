import akka.actor.Props

object ReduceWorkerImpl {
  def props(reducerId: String): Props = Props(new ReduceWorkerImpl(reducerId))

  case class RequestTrackReducer(name: String)
}

class ReduceWorkerImpl(name: String) extends ReduceWorker {
  override def receive: Receive = ???
}
