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
  case class FinalResponse(request: ReducersGroup.CalculateReduction, results: DirectionsResult)
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
      val value = calculateEuclideanMin(results)
      requester ! FinalResponse(request, value)
  }

  import com.google.maps.model.DirectionsResult
  import java.util

  def calculateEuclideanMin(result: util.Map[GeoPointPair, List[DirectionsResult]]): DirectionsResult =
    if (result.isEmpty) null
    else {
      var min = null
      val startGeoPoint = new GeoPoint(startLatitude, startLongitude)
      val endGeoPoint = new GeoPoint(endLatitude, endLongitude)
      import scala.collection.JavaConversions._
      for (entry <- result.entrySet) {
        if (min == null || min.getKey.getStartGeoPoint.euclideanDistance(startGeoPoint) + min.getKey.getEndGeoPoint.euclideanDistance(endGeoPoint) > entry.getKey.getStartGeoPoint.euclideanDistance(startGeoPoint) + entry.getKey.getEndGeoPoint.euclideanDistance(endGeoPoint)) min = entry
      }
      val minDirectionsResultList = if (min != null) min.getValue
      else null
      if (minDirectionsResultList != null && !minDirectionsResultList.isEmpty) {
        var minDirectionsResult = null
        import scala.collection.JavaConversions._
        for (directionsResult <- minDirectionsResultList) {
          if (minDirectionsResult == null) minDirectionsResult = directionsResult
          else {
            val totalDurationOfMin = Array(0)
            val totalDurationOfIteratee = Array(0)
            util.Arrays.stream(minDirectionsResult.routes).forEach((x: DirectionsRoute) => {
              def foo(x: DirectionsRoute) = util.Arrays.stream(x.legs).forEach((y: DirectionsLeg) => {
                def foo(y: DirectionsLeg) = {
                  totalDurationOfMin(0) += y.duration.inSeconds
                }

                foo(y)
              })

              foo(x)
            })
            util.Arrays.stream(directionsResult.routes).forEach((x: DirectionsRoute) => {
              def foo(x: DirectionsRoute) = util.Arrays.stream(x.legs).forEach((y: DirectionsLeg) => {
                def foo(y: DirectionsLeg) = {
                  totalDurationOfIteratee(0) += y.duration.inSeconds
                }

                foo(y)
              })

              foo(x)
            })
            if (totalDurationOfIteratee(0) < totalDurationOfMin(0)) minDirectionsResult = directionsResult
          }
        }
        minDirectionsResult
      }
      else null
    }
}

