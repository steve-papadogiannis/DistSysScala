package gr.papadogiannis.stefanos.masters

import gr.papadogiannis.stefanos.models.{CalculateDirections, CalculateReduction, ConcreteMapperResult, ConcreteReducerResult, FinalResponse, GeoPoint, GeoPointPair, MapperResult, ReducerResult, RequestTrackMapper, RequestTrackReducer, RespondAllMapResults, RespondAllReduceResults}
import com.google.maps.model.{DirectionsLeg, DirectionsResult, DirectionsRoute}
import gr.papadogiannis.stefanos.Main.CreateInfrastructure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.reducers.ReducersGroup
import gr.papadogiannis.stefanos.mappers.MappersGroup

object Master {
  def props(): Props = Props(new Master)
}

class Master extends Actor with ActorLogging {

  val reducersGroupActorName = "reducers-group-actor"
  val mappersGroupActorName = "mappers-group-actor"

  var reducersGroupActor: ActorRef = _

  var mappersGroupActor: ActorRef = _

  var requester: ActorRef = _

  override def preStart(): Unit = log.info("Master started")

  override def postStop(): Unit = log.info("Master stopped")

  override def receive: Receive = {
    case CreateInfrastructure =>
      log.info("Creating reducers group actor.")
      reducersGroupActor = context.actorOf(ReducersGroup.props(), reducersGroupActorName)
      reducersGroupActor ! RequestTrackReducer("moscow")
      log.info("Creating mappers group actor.")
      mappersGroupActor = context.actorOf(MappersGroup.props(this.self), mappersGroupActorName)
      context.watch(mappersGroupActor)
      mappersGroupActor ! RequestTrackMapper("havana")
      mappersGroupActor ! RequestTrackMapper("sao-paolo")
      mappersGroupActor ! RequestTrackMapper("athens")
      mappersGroupActor ! RequestTrackMapper("jamaica")
    case request@CalculateDirections(_, _, _, _, _) =>
      requester = sender()
      mappersGroupActor forward request
    case RespondAllMapResults(request, results) =>
      val merged = getMerged(results)
      reducersGroupActor ! CalculateReduction(request.requestId, merged)
    case RespondAllReduceResults(request, results) =>
      val merged = getMerged(results)
      val value = calculateEuclideanMin(merged)
      requester ! FinalResponse(request, value)
  }

  private def getMerged(results: Map[String, ReducerResult]) = {
    results.filter(x => x._2.isInstanceOf[ConcreteReducerResult])
      .foldLeft[Map[GeoPointPair, List[DirectionsResult]]](Map.empty[GeoPointPair, List[DirectionsResult]])((x, y) =>
        x + (y._1.asInstanceOf[GeoPointPair] ->
          y._2.asInstanceOf[ConcreteReducerResult].valueOption.asInstanceOf[List[DirectionsResult]]))
  }

  private def getMerged(results: Map[String, MapperResult]) = {
    results.filter(x => x._2.isInstanceOf[ConcreteMapperResult])
      .foldLeft[List[Map[GeoPointPair, DirectionsResult]]](List.empty[Map[GeoPointPair, DirectionsResult]])((x, y) =>
        x ++ y._2.asInstanceOf[ConcreteMapperResult].value)
  }

  import com.google.maps.model.DirectionsResult

  import java.util

  var startLatitude: Double = 30.0

  var startLongitude: Double = 23.0

  var endLatitude: Double = 29.0

  var endLongitude: Double = 43.0

  def calculateEuclideanMin(result: Map[GeoPointPair, List[DirectionsResult]]): DirectionsResult =
    if (result.isEmpty) null
    else {
      var min: Tuple2[GeoPointPair, List[DirectionsResult]] = null
      val startGeoPoint = new GeoPoint(startLatitude, startLongitude)
      val endGeoPoint = new GeoPoint(endLatitude, endLongitude)
      for ((key, value) <- result) {
        if (min == null || min._1.getStartGeoPoint.euclideanDistance(startGeoPoint) + min._1.getEndGeoPoint.euclideanDistance(endGeoPoint) >
          key.getStartGeoPoint.euclideanDistance(startGeoPoint) + key.getEndGeoPoint.euclideanDistance(endGeoPoint)) min = (key, value)
      }
      val minDirectionsResultList =
        if (min != null)
          min._2
        else
          null
      if (minDirectionsResultList != null && minDirectionsResultList.nonEmpty) {
        var minDirectionsResult: DirectionsResult = null
        for (directionsResult <- minDirectionsResultList) {
          if (minDirectionsResult == null)
            minDirectionsResult = directionsResult
          else {
            val totalDurationOfMin = Array(0L)
            val totalDurationOfIteratee = Array(0L)
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
            if (totalDurationOfIteratee(0) < totalDurationOfMin(0))
              minDirectionsResult = directionsResult
          }
        }
        minDirectionsResult
      }
      else null
    }

}

