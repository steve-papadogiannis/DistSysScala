package gr.papadogiannis.stefanos.mappers

import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings, Observable}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import akka.actor.{Actor, ActorLogging, Props}
import org.mongodb.scala.bson.codecs.Macros._
import gr.papadogiannis.stefanos.models._

object MapperWorker {
  def props(mapperId: String): Props = Props(new MapperWorker(mapperId))
}

class MapperWorker(mapperId: String) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("Mapper actor {} started", mapperId)

  override def postStop(): Unit = log.info("Mapper actor {} stopped", mapperId)

  override def receive: Receive = {
    case RequestTrackMapper(mapperName) =>
      sender() ! MapperRegistered(mapperName)
    case request@CalculateDirections(_, startLat, startLong, endLat, endLong) =>
      val senderActorRef = sender()
      map(GeoPoint(startLat, startLong), GeoPoint(endLat, endLong))
        .subscribe(finalResult => {
          senderActorRef ! RespondMapResults(request, finalResult.toList)
        }, throwable => {
          log.error(throwable.toString)
          log.error(throwable.getStackTrace.mkString("\n"))
        })
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

  def map(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): Observable[Seq[Map[GeoPointPair, DirectionsResult]]] = {
    val codecs = fromRegistries(
      fromProviders(
        classOf[DirectionsResultWrapper],
        classOf[GeoPoint],
        classOf[DirectionsResult],
        classOf[DirectionsRoute],
        classOf[DirectionsLeg],
        classOf[Duration],
        classOf[DirectionsStep],
        classOf[LatLng],
        classOf[EncodedPolyline]
      ),
      DEFAULT_CODEC_REGISTRY)
    val mongoClientSettings = MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString("mongodb://root:example@localhost:27017"))
      .codecRegistry(codecs)
      .build()
    val mongoClient = MongoClient(mongoClientSettings)
    val database = mongoClient.getDatabase("dist-sys")
    val collection = database.getCollection("directions")
    val ipPortHash = calculateHash("127.0.0.1" + 5555)
    val ipPortHashMod4 = if (ipPortHash % 4 < 0)
      -(ipPortHash % 4)
    else
      ipPortHash % 4
    collection.find[DirectionsResultWrapper]().filter(x => {
        val geoPointsHash = calculateHash(x.startPoint.latitude.toString + x.startPoint.longitude.toString +
          x.endPoint.latitude.toString + x.endPoint.longitude.toString)
        val geoPointsHashMod4 = if (geoPointsHash % 4 < 0)
          -(geoPointsHash % 4)
        else
          geoPointsHash % 4
        ipPortHashMod4 == geoPointsHashMod4
    }).map(x => {
        val map = Map.empty[GeoPointPair, DirectionsResult]
        val isStartLatitudeNearIssuedStartLatitude = Math.abs(startGeoPoint.latitude - x.startPoint.latitude) < 0.0001
        val isStartLongitudeNearIssuedStartLongitude = Math.abs(startGeoPoint.longitude - x.startPoint.longitude) < 0.0001
        val isEndLatitudeNearIssuedEndLatitude = Math.abs(endGeoPoint.latitude - x.endPoint.latitude) < 0.0001
        val isEndLongitudeNearIssuedEndLongitude = Math.abs(endGeoPoint.longitude - x.endPoint.longitude) < 0.0001
        if (isStartLatitudeNearIssuedStartLatitude &&
          isStartLongitudeNearIssuedStartLongitude &&
          isEndLatitudeNearIssuedEndLatitude &&
          isEndLongitudeNearIssuedEndLongitude) {
          val geoPointPair = new GeoPointPair(new GeoPoint(roundTo2Decimals(x.startPoint.latitude),
            roundTo2Decimals(x.startPoint.longitude)), new GeoPoint(roundTo2Decimals(x.endPoint.latitude),
            roundTo2Decimals(x.endPoint.longitude)))
          map + (geoPointPair -> x.directionsResult)
        }
        map
    }).filter(x => x.nonEmpty).collect()
  }

}
