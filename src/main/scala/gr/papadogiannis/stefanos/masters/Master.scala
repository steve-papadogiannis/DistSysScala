package gr.papadogiannis.stefanos.masters

import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPoint, GeoPointPair, LatLng}
import gr.papadogiannis.stefanos.integrations.GoogleDirectionsAPIActor
import gr.papadogiannis.stefanos.constants.ApplicationConstants._
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.reducers.ReducersGroup
import gr.papadogiannis.stefanos.mappers.MappersGroup
import gr.papadogiannis.stefanos.repos.MongoActor
import gr.papadogiannis.stefanos.caches.MemCache
import gr.papadogiannis.stefanos.messages._

import scala.collection.mutable.ListBuffer

object Master {
  def props(): Props = Props(new Master)
}

class Master extends Actor with ActorLogging {

  var googleDirectionsAPIActor: ActorRef = _
  var reducersGroupActor: ActorRef = _
  var mappersGroupActor: ActorRef = _
  var memCacheActor: ActorRef = _
  var mongoActor: ActorRef = _

  var requestIdToRequester = Map.empty[Long, ActorRef]

  override def preStart(): Unit = log.info("Master started")

  override def postStop(): Unit = log.info("Master stopped")

  override def receive: Receive = {
    case CreateInfrastructure =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(CreateInfrastructure))
      log.info("Creating mem cache actor.")
      memCacheActor = context.actorOf(MemCache.props(), MEM_CACHE_ACTOR_NAME)
      log.info("Creating google directions API actor.")
      googleDirectionsAPIActor = context.actorOf(GoogleDirectionsAPIActor.props(), GOOGLE_DIRECTIONS_API_ACTOR_NAME)
      log.info("Creating google directions API actor.")
      mongoActor = context.actorOf(MongoActor.props(), MONGO_ACTOR_NAME)
      log.info("Creating reducers group actor.")
      reducersGroupActor = context.actorOf(ReducersGroup.props(), REDUCERS_GROUP_ACTOR_NAME)
      reducersGroupActor ! RequestTrackReducer("moscow")
      log.info("Creating mappers group actor.")
      mappersGroupActor = context.actorOf(MappersGroup.props(this.self, mongoActor), MAPPERS_GROUP_ACTOR_NAME)
      context.watch(mappersGroupActor)
      mappersGroupActor ! RequestTrackMapper("havana", 0, 4)
      mappersGroupActor ! RequestTrackMapper("sao-paolo", 1, 4)
      mappersGroupActor ! RequestTrackMapper("athens", 2, 4)
      mappersGroupActor ! RequestTrackMapper("jamaica", 3, 4)
    case request@CalculateDirections(requestId, _) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      requestIdToRequester = requestIdToRequester + (requestId -> sender())
      memCacheActor ! CacheCheck(request)
    case request@CacheHit(calculateDirections, directionsResult) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      processResponse(CalculateReduction(calculateDirections, List.empty), Some(directionsResult))
    case request@CacheMiss(calculateDirections) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      mappersGroupActor forward calculateDirections
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
          processResponse(calculateReduction, valueOption)
        case None =>
          googleDirectionsAPIActor ! GetDirections(calculateReduction)
      }
    case request@GoogleAPIResponse(calculateReduction, maybeResult) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      maybeResult match {
        case Some(directionsResult@DirectionsResult(_)) =>
          memCacheActor ! UpdateCache(calculateReduction.request.geoPointPair, directionsResult)
          mongoActor ! UpdateDB(calculateReduction.request.geoPointPair, directionsResult)
        case None =>
      }
      processResponse(calculateReduction, maybeResult)
  }

  private def processResponse(calculateReduction: CalculateReduction, valueOption: Option[DirectionsResult]): Unit = {
    val actorRefOption = requestIdToRequester.get(calculateReduction.request.requestId)
    actorRefOption.map(actorRef => actorRef ! FinalResponse(calculateReduction, valueOption.map(toListOfLatLng)))
      .getOrElse(log.warning(s"The actorRef for ${calculateReduction.request.requestId} was not found"))
    requestIdToRequester = requestIdToRequester - calculateReduction.request.requestId
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

  private def toListOfLatLng(directionsResult: DirectionsResult) = {
    directionsResult.routes.flatMap(_.legs.toStream)
      .flatMap(leg => {
        val list = ListBuffer.empty[LatLng]
        list += LatLng(leg.startLocation.lat, leg.startLocation.lng)
        leg.steps.flatMap(_.polyline.decodePath).foreach(latLng => {
          list += LatLng(latLng.lat, latLng.lng)
        })
        list += LatLng(leg.endLocation.lat, leg.endLocation.lng)
        list
      })
  }

}

