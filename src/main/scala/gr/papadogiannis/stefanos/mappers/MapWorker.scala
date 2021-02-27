package gr.papadogiannis.stefanos.mappers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.maps.model.DirectionsResult
import gr.papadogiannis.stefanos.models.{DirectionsResultWrapper, GeoPoint, GeoPointPair}
import MapWorker.{MapperRegistered, RespondMapResults}
import gr.papadogiannis.stefanos.masters.Master.RequestTrackMapper
import gr.papadogiannis.stefanos.server.models.GeoPoint
import gr.papadogiannis.stefanos.servers.Server.CalculateDirections

import scala.io.Source

object MapWorker {

  def props(mapperId: String, reducersGroupActorRef: ActorRef, masterActorRef: ActorRef): Props =
    Props(new MapWorker(mapperId, reducersGroupActorRef, masterActorRef))

  final case class RecordTemperature(requestId: Long, value: Double)

  final case class TemperatureRecorded(requestId: Long)

  final case class ReadTemperature(requestId: Long)

  final case class RespondTemperature(requestId: Long, value: Option[Double])

  object MapperRegistered

  object AcknowledgeMaster

  case class CalculateReduction(finalResult: Any)

  case class RespondMapResults(request: CalculateDirections, valueOption: List[Map[GeoPointPair, DirectionsResult]])

}

class MapWorker(mapperId: String, reducersGroupActorRef: ActorRef, masterActorRef: ActorRef)
  extends Actor
    with ActorLogging {

  override def preStart(): Unit = log.info("Mapper actor {} started", mapperId)

  override def postStop(): Unit = log.info("Mapper actor {} stopped", mapperId)

  override def receive: Receive = {
    case RequestTrackMapper(_) =>
      sender() ! MapperRegistered
    case request@CalculateDirections(requestId, startLat, startLong, endLat, endLong) =>
      val finalResult = map(new GeoPoint(startLat, startLong), new GeoPoint(endLat, endLong))
      sender() ! RespondMapResults(request, finalResult)
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
    val decimalFormat = new DecimalFormat("###.##")
    decimalFormat.format(number).toDouble
  }

  def map(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): List[Map[GeoPointPair, DirectionsResult]] = {
    val filename = "5555_directions"
    val mapper = new ObjectMapper()
    val result: List[DirectionsResultWrapper] = Source
      .fromFile(filename)
      .getLines.map(line =>
      mapper.readValue(line, classOf[DirectionsResultWrapper]))
      .toList
    val ipPortHash = calculateHash("127.0.0.1" + 5555)
    val ipPortHashMod4 = if (ipPortHash % 4 < 0)
      -(ipPortHash % 4)
    else
      ipPortHash % 4
    val resultsThatThisWorkerIsInChargeOf = result.filter(x => {
      def foo(x: DirectionsResultWrapper) = {
        val geoPointsHash = calculateHash(x.startPoint.getLatitude.toString + x.startPoint.getLongitude.toString +
          x.endPoint.getLatitude.toString + x.endPoint.getLongitude.toString)
        val geoPointsHashMod4 = if (geoPointsHash % 4 < 0)
          -(geoPointsHash % 4)
        else
          geoPointsHash % 4
        ipPortHashMod4 == geoPointsHashMod4
      }

      foo(x)
    })
    val finalResult: List[Map[GeoPointPair, DirectionsResult]] = resultsThatThisWorkerIsInChargeOf.map(x => {
      def foo(x: DirectionsResultWrapper): Map[GeoPointPair, DirectionsResult] = {
        val map = Map.empty[GeoPointPair, DirectionsResult]
        val isStartLatitudeNearIssuedStartLatitude = Math.abs(startGeoPoint.getLatitude - x.startPoint.getLatitude) < 0.0001
        val isStartLongitudeNearIssuedStartLongitude = Math.abs(startGeoPoint.getLongitude - x.startPoint.getLongitude) < 0.0001
        val isEndLatitudeNearIssuedEndLatitude = Math.abs(endGeoPoint.getLatitude - x.endPoint.getLatitude) < 0.0001
        val isEndLongitudeNearIssuedEndLongitude = Math.abs(endGeoPoint.getLongitude - x.endPoint.getLongitude) < 0.0001
        if (isStartLatitudeNearIssuedStartLatitude &&
          isStartLongitudeNearIssuedStartLongitude &&
          isEndLatitudeNearIssuedEndLatitude &&
          isEndLongitudeNearIssuedEndLongitude) {
          val geoPointPair = new GeoPointPair(new GeoPoint(roundTo2Decimals(x.startPoint.getLatitude),
            roundTo2Decimals(x.startPoint.getLongitude)), new GeoPoint(roundTo2Decimals(x.endPoint.getLatitude),
            roundTo2Decimals(x.endPoint.getLongitude)))
          map + (geoPointPair -> x.directionsResult)
        }
        map
      }

      foo(x)
    }).filter(x => x.nonEmpty)
    finalResult
  }

}
