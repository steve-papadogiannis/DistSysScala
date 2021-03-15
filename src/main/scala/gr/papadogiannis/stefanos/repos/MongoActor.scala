package gr.papadogiannis.stefanos.repos

import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings, MongoCollection, MongoDatabase}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import gr.papadogiannis.stefanos.messages.{DBResult, FindAll, UpdateDB}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.connection.SocketSettings
import akka.actor.{Actor, ActorLogging, Props}
import org.mongodb.scala.bson.codecs.Macros._
import gr.papadogiannis.stefanos.models._
import org.mongodb.scala.model.Filters

import java.util.concurrent.TimeUnit

object MongoActor {
  def props(): Props = Props(new MongoActor)
}

class MongoActor extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.info("MongoActor started")
    val codecRegistries = getCodecRegistries
    val socketSettings = getSocketSettings
    val mongoClientSettings = getMongoClientSettings(socketSettings)
    val mongoClient = getMongoClient(mongoClientSettings)
    val database = getMongoDatabase(mongoClient, codecRegistries)
    collection = getMongoCollection(database)
  }

  override def postStop(): Unit = log.info("MongoActor stopped")

  var collection: MongoCollection[DirectionsResultWrapper] = _

  override def receive: Receive = {
    case request@FindAll(calculateDirections, mapperId, noOfMappers) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      val ref = sender()
      val pair = calculateDirections.geoPointPair
      val startGeoPoint = pair.startGeoPoint
      val endGeoPoint = pair.endGeoPoint
      collection.find(Filters.where(
        s"Math.abs(this.startPoint.latitude - ${startGeoPoint.latitude}) < 0.0001 && " +
        s"Math.abs(this.startPoint.longitude - ${startGeoPoint.longitude}) < 0.0001 && " +
        s"Math.abs(this.endPoint.latitude - ${endGeoPoint.latitude}) < 0.0001 && " +
        s"Math.abs(this.endPoint.longitude - ${endGeoPoint.longitude}) < 0.0001 && " +
        s"(${mapperId} === (Array.from(hex_md5((this.startPoint.latitude + this.startPoint.longitude + this.endPoint.latitude + this.endPoint.longitude).toString())).map(elem => elem.codePointAt()).reduce((acc, elem) => acc + elem, 0) % ${noOfMappers}))"))
        .collect()
        .subscribe(finalResult => {
          ref ! DBResult(calculateDirections, finalResult.toList)
        }, throwable => {
          log.error(throwable.toString)
          log.error(throwable.getStackTrace.mkString("\n"))
        })
    case message@UpdateDB(geoPointPair: GeoPointPair, directionsResult: DirectionsResult) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val wrapper = DirectionsResultWrapper(geoPointPair.startGeoPoint, geoPointPair.endGeoPoint, directionsResult)
      collection.insertOne(wrapper).subscribe(insertedId => {
        log.info(s"Inserted new document with id: $insertedId")
      }, throwable => {
        log.error(throwable.toString)
        log.error(throwable.getStackTrace.mkString("\n"))
      })
  }

  private def getMongoCollection(database: MongoDatabase): MongoCollection[DirectionsResultWrapper] = {
    database.getCollection("directions")
  }

  private def getMongoDatabase(mongoClient: MongoClient, codecRegistry: CodecRegistry) = {
    mongoClient.getDatabase("dist-sys").withCodecRegistry(codecRegistry)
  }

  private def getMongoClient(mongoClientSettings: MongoClientSettings) = {
    MongoClient(mongoClientSettings)
  }

  private def getSocketSettings() = {
    SocketSettings
      .builder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .build()
  }

  private def getMongoClientSettings(socketSettings: SocketSettings) = {
    MongoClientSettings
      .builder()
      .applyConnectionString(ConnectionString(getMongoConnectionString))
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
