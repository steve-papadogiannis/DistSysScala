package gr.papadogiannis.stefanos.mappers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.{DECIMAL_FORMAT, RECEIVED_MESSAGE_PATTERN}
import gr.papadogiannis.stefanos.messages._
import gr.papadogiannis.stefanos.models._

object MapperWorker {
  def props(mapperId: Long, mapperName: String, noOfMappers: Long, mongoActorRef: ActorRef): Props =
    Props(new MapperWorker(mapperId, mapperName, noOfMappers, mongoActorRef))
}

class MapperWorker(mapperId: Long, mapperName: String, noOfMappers: Long, mongoActorRef: ActorRef) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("Mapper actor {} started", mapperName)

  override def postStop(): Unit = log.info("Mapper actor {} stopped", mapperName)

  var requestIdToRequester = Map.empty[Long, ActorRef]

  override def receive: Receive = {
    case message@RequestTrackMapper(mapperName, _, _) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      sender() ! MapperRegistered(mapperName)
    case message@CalculateDirections(requestId, _) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      requestIdToRequester = requestIdToRequester + (requestId -> sender())
      mongoActorRef ! FindAll(message, mapperId, noOfMappers)
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

  def roundTo2Decimals(number: Double): Double = {
    import java.text.DecimalFormat
    val decimalFormat = new DecimalFormat(DECIMAL_FORMAT)
    decimalFormat.format(number).toDouble
  }

  def map(geoPointPair: GeoPointPair, directionsResultWrappers: List[DirectionsResultWrapper]): Seq[Map[GeoPointPair, DirectionsResult]] = {
    directionsResultWrappers
      .map(directionsResultWrapper => Map(geoPointPair -> directionsResultWrapper.directionsResult))
      .filter(map => map.nonEmpty)
  }

}
