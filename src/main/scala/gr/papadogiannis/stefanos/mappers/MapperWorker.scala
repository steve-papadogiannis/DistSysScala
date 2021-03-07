package gr.papadogiannis.stefanos.mappers

import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings, MongoDatabase, Observable}
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistry
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
    val codecRegistries = getCodecRegistries
    val mongoClientSettings = getMongoClientSettings(codecRegistries)
    val mongoClient = getMongoClient(mongoClientSettings)
    val database = getMongoDatabase(mongoClient)
    val collection = getMongoCollection(database)
    val mapperHash: Long = getMapperHash
    collection
      .find[DirectionsResultWrapper]()
      .filter(x => {
        val geoPointsHashMod4: Long = getGeoPointsHash(x)
        mapperHash == geoPointsHashMod4
      })
      .map(x => {
        val map = Map.empty[GeoPointPair, DirectionsResult]
        val isStartLatitudeNearIssuedStartLatitude = Math.abs(startGeoPoint.latitude - x.startPoint.latitude) < 0.0001
        val isStartLongitudeNearIssuedStartLongitude = Math.abs(startGeoPoint.longitude - x.startPoint.longitude) < 0.0001
        val isEndLatitudeNearIssuedEndLatitude = Math.abs(endGeoPoint.latitude - x.endPoint.latitude) < 0.0001
        val isEndLongitudeNearIssuedEndLongitude = Math.abs(endGeoPoint.longitude - x.endPoint.longitude) < 0.0001
        if (isStartLatitudeNearIssuedStartLatitude &&
          isStartLongitudeNearIssuedStartLongitude &&
          isEndLatitudeNearIssuedEndLatitude &&
          isEndLongitudeNearIssuedEndLongitude) {
          val geoPointPair = new GeoPointPair(
            GeoPoint(
              roundTo2Decimals(x.startPoint.latitude),
              roundTo2Decimals(x.startPoint.longitude)),
            GeoPoint(
              roundTo2Decimals(x.endPoint.latitude),
              roundTo2Decimals(x.endPoint.longitude)))
          map + (geoPointPair -> x.directionsResult)
        }
        map
      })
      .filter(x => x.nonEmpty)
      .collect()
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
      .applyConnectionString(new ConnectionString(getMongoConnectionString))
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
