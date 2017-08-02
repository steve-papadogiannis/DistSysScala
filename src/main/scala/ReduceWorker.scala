import akka.actor.{Actor, ActorLogging, Props}
import com.google.maps.model.DirectionsResult

object ReduceWorker {
  def props(reducerId: String): Props = Props(new ReduceWorker(reducerId))
  case class ReadReduceResult(i: Int)
  sealed trait ReducerResult
  case class RespondeReduceResult(requestId: Long, value: Map[GeoPointPair, List[DirectionsResult]]) extends ReducerResult
}

class ReduceWorker(name: String) extends Actor with ActorLogging {
  override def receive: Receive = ???
}
