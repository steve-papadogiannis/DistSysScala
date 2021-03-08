package gr.papadogiannis.stefanos.masters

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import gr.papadogiannis.stefanos.caches.MemCache
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.integrations.GoogleDirectionsAPIActor
import gr.papadogiannis.stefanos.mappers.MappersGroup
import gr.papadogiannis.stefanos.messages._
import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPoint, GeoPointPair}
import gr.papadogiannis.stefanos.reducers.ReducersGroup

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object Master {
  def props(): Props = Props(new Master)
}

class Master extends Actor with ActorLogging {

  val googleDirectionsAPIActorName = "google-directions-api-actor"
  val reducersGroupActorName = "reducers-group-actor"
  val mappersGroupActorName = "mappers-group-actor"
  val memCacheActorName = "mem-cache-actor"

  var googleDirectionsAPIActor: ActorRef = _
  var reducersGroupActor: ActorRef = _
  var mappersGroupActor: ActorRef = _
  var memCacheActor: ActorRef = _
  var requester: ActorRef = _

  override def preStart(): Unit = log.info("Master started")

  override def postStop(): Unit = log.info("Master stopped")

  override def receive: Receive = {
    case CreateInfrastructure =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(CreateInfrastructure))
      log.info("Creating mem cache actor.")
      memCacheActor = context.actorOf(MemCache.props(), memCacheActorName)
      log.info("Creating google directions API actor.")
      googleDirectionsAPIActor = context.actorOf(GoogleDirectionsAPIActor.props(), googleDirectionsAPIActorName)
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
    case request@CalculateDirections(requestId, geoPointPair) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      requester = sender()
      implicit val askTimeout: Timeout = Timeout(10.seconds)
      val future = memCacheActor ? CacheCheck(geoPointPair)
      future.onComplete {
        case Success(directionsResultOption) =>
          directionsResultOption match {
            case Some(directionsResult@DirectionsResult(_)) =>
              requester ! FinalResponse(CalculateReduction(request, List.empty), Some(directionsResult))
            case None =>
              log.info("MemCache check missed, sending to mappers group")
              mappersGroupActor forward request
          }
        case Failure(exception) =>
          log.error(exception.toString)
          mappersGroupActor forward request
      }
    case message@RespondAllMapResults(request, results) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val merged = getMerged(results)
      reducersGroupActor ! CalculateReduction(request, merged)
    case message@RespondAllReduceResults(calculateReduction, results) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val merged = getMerged(results)
      val valueOption = calculateEuclideanMin(merged)
      val geoPointPair = calculateReduction.request.geoPointPair
      valueOption match {
        case Some(value) =>
          memCacheActor ! UpdateCache(geoPointPair, value)
          requester ! FinalResponse(calculateReduction, valueOption)
        case None =>
          implicit val askTimeout: Timeout = Timeout(10.seconds)
          val future = googleDirectionsAPIActor ? GetDirections(geoPointPair)
          future.onComplete {
            case Success(maybeResult: Option[DirectionsResult]) =>
              maybeResult match {
                case Some(directionsResult@DirectionsResult(_)) =>
                  memCacheActor ! UpdateCache(geoPointPair, directionsResult)
                case None =>
              }
              requester ! FinalResponse(calculateReduction, maybeResult)
            case Failure(exception) =>
              log.error(exception.toString)
          }
      }
  }

  private def getMerged(results: Map[String, ReducerResult]) = {
    results.values
      .filter(value => value.isInstanceOf[ConcreteReducerResult])
      .map(value => value.asInstanceOf[ConcreteReducerResult])
      .foldLeft[Map[GeoPointPair, List[DirectionsResult]]](Map.empty[GeoPointPair, List[DirectionsResult]])((accumulator, concreteReducerResult) => {
        accumulator ++ concreteReducerResult.valueOption
      })
  }

  private def getMerged(results: Map[String, MapperResult]) = {
    results.filter(x => x._2.isInstanceOf[ConcreteMapperResult])
      .foldLeft[List[Map[GeoPointPair, DirectionsResult]]](List.empty[Map[GeoPointPair, DirectionsResult]])((x, y) =>
        x ++ y._2.asInstanceOf[ConcreteMapperResult].value)
  }

  def getMinimum(result: Map[GeoPointPair, List[DirectionsResult]]): Option[(GeoPointPair, List[DirectionsResult])] = {
    val startLatitude: Double = 30.0
    val startLongitude: Double = 23.0
    val endLatitude: Double = 29.0
    val endLongitude: Double = 43.0
    val startReferencePoint = GeoPoint(startLatitude, startLongitude)
    val endReferencePoint = GeoPoint(endLatitude, endLongitude)
    result.reduceLeftOption((previousMin, pair) => {
      val sumPreviousPair = getEuclideanDistance(startReferencePoint, previousMin) + getEuclideanDistance(endReferencePoint, previousMin)
      val sumPair = getEuclideanDistance(startReferencePoint, pair) + getEuclideanDistance(endReferencePoint, pair)
      if (sumPreviousPair > sumPair)
        pair
      else
        previousMin
    })
  }

  private def getEuclideanDistance(referenceGeoPoint: GeoPoint, tuple: (GeoPointPair, List[DirectionsResult])) = {
    tuple._1.startGeoPoint.euclideanDistance(referenceGeoPoint)
  }

  def calculateEuclideanMin(result: Map[GeoPointPair, List[DirectionsResult]]): Option[DirectionsResult] = {
    val min: Option[(GeoPointPair, List[DirectionsResult])] = getMinimum(result);
    val minDirectionsResultList = min.map(_._2)
    minDirectionsResultList.map(_.reduceLeft((previousMinDirectionsResult, currentDirectionsResult) => {
      val totalDurationOfMin = getTotalDuration(previousMinDirectionsResult)
      val totalDurationOfIteratee = getTotalDuration(currentDirectionsResult)
      if (totalDurationOfIteratee < totalDurationOfMin)
        currentDirectionsResult
      else
        previousMinDirectionsResult
    }))
  }

  private def getTotalDuration(directionsResult: DirectionsResult) = {
    directionsResult.routes.foldLeft(0L)((accumulator, directionsRoute) => {
      accumulator + directionsRoute.legs.foldLeft(0L)((accumulatorInner, directionsLeg) => {
        accumulatorInner + directionsLeg.duration.inSeconds
      })
    })
  }

}

