import AndroidServer.CalculateDirections
import Main.CreateInfrastracture
import MapWorker.CalculateReduction
import MappersGroup.RespondAllMapResults
import Master.{FinalResponse, RequestTrackMapper, RequestTrackReducer}
import ReducersGroup.RespondAllReduceResults
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.google.maps.model.DirectionsResult

object Master {
  def props: Props = Props(new Master)
  final case class RequestTrackMapper(str: String)
  final case class RequestTrackReducer(a: String)
  case class FinalResponse(request: ReducersGroup.CalculateReduction, results: Map[String, ReducersGroup.ReducerResult])
}

class Master extends Actor with ActorLogging {
  var mappersGroupActor: ActorRef = _
  var reducersGroupActor: ActorRef = _
  var requester: ActorRef = _
  override def preStart(): Unit = log.info("MasterImpl started")
  override def postStop(): Unit = log.info("MasterImpl stopped")
  override def receive: Receive = {
    case CreateInfrastracture =>
      log.info("Creating reducers group actor.")
      reducersGroupActor = context.actorOf(ReducersGroup.props(mappersGroupActor, this.self))
      reducersGroupActor ! RequestTrackReducer("moscow")
      log.info("Creating mappers group actor.")
      mappersGroupActor = context.actorOf(MappersGroup.props(reducersGroupActor, this.self))
      context.watch(mappersGroupActor)
      mappersGroupActor ! RequestTrackMapper("havana")
      mappersGroupActor ! RequestTrackMapper("saoPaolo")
      mappersGroupActor ! RequestTrackMapper("athens")
      mappersGroupActor ! RequestTrackMapper("jamaica")
    case request @ CalculateDirections =>
      requester = sender()
      mappersGroupActor forward request
    case RespondAllMapResults(request, results) =>
      val merged = results.filter(x => x._2.isInstanceOf[MappersGroup.ConcreteResult])
    .foldLeft[List[Map[GeoPointPair, DirectionsResult]]](List.empty[Map[GeoPointPair, DirectionsResult]])((x, y) =>
      x ++ y._2.asInstanceOf[MappersGroup.ConcreteResult].value)
      reducersGroupActor ! CalculateReduction(request.requestId, merged)
    case RespondAllReduceResults(request, results) =>
      requester ! FinalResponse(request, results)
  }
}

