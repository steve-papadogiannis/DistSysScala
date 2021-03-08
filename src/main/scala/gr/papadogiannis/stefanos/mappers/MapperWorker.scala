package gr.papadogiannis.stefanos.mappers

import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings, MongoDatabase, Observable}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistry
import akka.actor.{Actor, ActorLogging, Props}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages.{CalculateDirections, MapperRegistered, RequestTrackMapper, RespondMapResults}
import org.mongodb.scala.bson.codecs.Macros._
import gr.papadogiannis.stefanos.models._

object MapperWorker {
  def props(mapperId: String): Props = Props(new MapperWorker(mapperId))
}

class MapperWorker(mapperId: String) extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("Mapper actor {} started", mapperId)

  override def postStop(): Unit = log.info("Mapper actor {} stopped", mapperId)

  override def receive: Receive = {
    case message@RequestTrackMapper(mapperName) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      sender() ! MapperRegistered(mapperName)
    case message@CalculateDirections(_, startLat, startLong, endLat, endLong) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val senderActorRef = sender()
      map(GeoPoint(startLat, startLong), GeoPoint(endLat, endLong))
        .subscribe(finalResult => {
          senderActorRef ! RespondMapResults(message, finalResult.toList)
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
    val codecRegistries = getCodecRegistries
    val mongoClientSettings = getMongoClientSettings(codecRegistries)
    val mongoClient = getMongoClient(mongoClientSettings)
    val database = getMongoDatabase(mongoClient)
    val collection = getMongoCollection(database)
    val mapperHash: Long = getMapperHash
    collection
      .find[DirectionsResultWrapper]()
      .filter(directionsResultWrapper => {
        val geoPointsHashMod4: Long = getGeoPointsHash(directionsResultWrapper)
        mapperHash == geoPointsHashMod4
      })
      .map(directionsResultWrapper => {
        val map = Map.empty[GeoPointPair, DirectionsResult]
        val isStartLatitudeNearIssuedStartLatitude = checkIfNear(startGeoPoint.latitude, directionsResultWrapper.startPoint.latitude)
        val isStartLongitudeNearIssuedStartLongitude = checkIfNear(startGeoPoint.longitude, directionsResultWrapper.startPoint.longitude)
        val isEndLatitudeNearIssuedEndLatitude = checkIfNear(endGeoPoint.latitude, directionsResultWrapper.endPoint.latitude)
        val isEndLongitudeNearIssuedEndLongitude = checkIfNear(endGeoPoint.longitude, directionsResultWrapper.endPoint.longitude)
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
      .collect()
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

  private def getMongoCollection(database: MongoDatabase) = {
    database.getCollection("directions")
  }

  private def getMongoDatabase(mongoClient: MongoClient) = {
    mongoClient.getDatabase("dist-sys")
  }

  private def getMongoClient(mongoClientSettings: MongoClientSettings) = {
    MongoClient(mongoClientSettings)
  }

  private def getMongoClientSettings(codecRegistries: CodecRegistry) = {
    MongoClientSettings
      .builder()
      .applyConnectionString(ConnectionString(getMongoConnectionString))
      .codecRegistry(codecRegistries)
      .build()
  }

  private def getMongoConnectionString = {
    "mongodb://root:example@localhost:27017"
  }

  private def getCodecRegistries = {
    fromRegistries(
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
  }

}
