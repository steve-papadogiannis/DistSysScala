package gr.papadogiannis.stefanos.mappers

import gr.papadogiannis.stefanos.messages.{CalculateDirections, DBResult, FindAll, MapperRegistered, RequestTrackMapper, RespondMapResults}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.{DECIMAL_FORMAT, RECEIVED_MESSAGE_PATTERN}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.models._

object MapperWorker {
  def props(mapperId: String, mongoActorRef: ActorRef): Props = Props(new MapperWorker(mapperId, mongoActorRef))
}

class MapperWorker(mapperId: String, mongoActorRef: ActorRef) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("Mapper actor {} started", mapperId)

  override def postStop(): Unit = log.info("Mapper actor {} stopped", mapperId)

  var requestIdToRequester = Map.empty[Long, ActorRef]

  override def receive: Receive = {
    case message@RequestTrackMapper(mapperName) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      sender() ! MapperRegistered(mapperName)
    case message@CalculateDirections(requestId, _) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      requestIdToRequester = requestIdToRequester + (requestId -> sender())
      mongoActorRef ! FindAll(message)
    case message@DBResult(calculateDirections, directionsResultWrappers) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val pairToResults = map(calculateDirections.geoPointPair, directionsResultWrappers)
      processResponse(calculateDirections, pairToResults.toList)
  }

  private def processResponse(calculateDirections: CalculateDirections,
                              directionsResults: List[Map[GeoPointPair, DirectionsResult]]): Unit = {
    val actorRefOption = requestIdToRequester.get(calculateDirections.requestId)
    actorRefOption.map(actorRef => actorRef ! RespondMapResults(calculateDirections, directionsResults))
      .getOrElse(log.warning(s"The actorRef for ${calculateDirections.requestId} was not found"))
    requestIdToRequester = requestIdToRequester - calculateDirections.requestId
  }

  def calculateHash(str: String): Long = {
    import java.io.UnsupportedEncodingException
    import java.nio.ByteBuffer
    import java.security.{MessageDigest, NoSuchAlgorithmException}
    var bytesOfMessage: Array[Byte] = new Array[Byte](0)
    try {
      bytesOfMessage = str.getBytes("UTF-8")
    } catch {
      case e: UnsupportedEncodingException =>
        e.printStackTrace()
    }
    var md: MessageDigest = null
    try {
      md = MessageDigest.getInstance("MD5")
    } catch {
      case e: NoSuchAlgorithmException =>
        e.printStackTrace()
    }
    val thedigest: Array[Byte] = if (md != null) {
      md.digest(bytesOfMessage)
    }
    else {
      new Array[Byte](0)
    }
    val bb: ByteBuffer = ByteBuffer.wrap(thedigest)
    bb.getLong
  }

  def roundTo2Decimals(number: Double): Double = {
    import java.text.DecimalFormat
    val decimalFormat = new DecimalFormat(DECIMAL_FORMAT)
    decimalFormat.format(number).toDouble
  }

  def map(geoPointPair: GeoPointPair, directionsResultWrappers: List[DirectionsResultWrapper]): Seq[Map[GeoPointPair, DirectionsResult]] = {
    val mapperHash: Long = getMapperHash
    directionsResultWrappers
      .filter(directionsResultWrapper => {
        val geoPointsHashMod4: Long = getGeoPointsHash(directionsResultWrapper)
        mapperHash == geoPointsHashMod4
      })
      .map(directionsResultWrapper => {
        val map = Map.empty[GeoPointPair, DirectionsResult]
        val isStartLatitudeNearIssuedStartLatitude = checkIfNear(
          geoPointPair.startGeoPoint.latitude,
          directionsResultWrapper.startPoint.latitude)
        val isStartLongitudeNearIssuedStartLongitude = checkIfNear(
          geoPointPair.startGeoPoint.longitude,
          directionsResultWrapper.startPoint.longitude)
        val isEndLatitudeNearIssuedEndLatitude = checkIfNear(
          geoPointPair.endGeoPoint.latitude,
          directionsResultWrapper.endPoint.latitude)
        val isEndLongitudeNearIssuedEndLongitude = checkIfNear(
          geoPointPair.endGeoPoint.longitude,
          directionsResultWrapper.endPoint.longitude)
        if (isStartLatitudeNearIssuedStartLatitude &&
          isStartLongitudeNearIssuedStartLongitude &&
          isEndLatitudeNearIssuedEndLatitude &&
          isEndLongitudeNearIssuedEndLongitude) {
          val geoPointPair = GeoPointPair(
            GeoPoint(
              roundTo2Decimals(directionsResultWrapper.startPoint.latitude),
              roundTo2Decimals(directionsResultWrapper.startPoint.longitude)),
            GeoPoint(
              roundTo2Decimals(directionsResultWrapper.endPoint.latitude),
              roundTo2Decimals(directionsResultWrapper.endPoint.longitude)))
          map + (geoPointPair -> directionsResultWrapper.directionsResult)
        }
        map
      })
      .filter(map => map.nonEmpty)
  }

  private def checkIfNear(firstPoint: Double, secondPoint: Double) = {
    Math.abs(firstPoint - secondPoint) < 0.0001
  }

  private def getGeoPointsHash(x: DirectionsResultWrapper) = {
    val geoPointsHash = calculateHash(
      x.startPoint.latitude.toString +
        x.startPoint.longitude.toString +
        x.endPoint.latitude.toString +
        x.endPoint.longitude.toString)
    val geoPointsHashModFour = geoPointsHash % 4
    if (geoPointsHashModFour < 0) -geoPointsHashModFour else geoPointsHashModFour
  }

  private def getMapperHash = {
    val mapperHash = calculateHash(mapperId)
    val mapperHashModFour = mapperHash % 4
    if (mapperHashModFour < 0) -mapperHashModFour else mapperHashModFour
  }

}
