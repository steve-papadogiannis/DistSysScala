package gr.papadogiannis.stefanos.repos

import akka.actor.{Actor, ActorLogging, Props}
import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.messages.{DBResult, FindAll, UpdateDB}
import gr.papadogiannis.stefanos.models._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.connection.SocketSettings
import org.mongodb.scala.{ConnectionString, Document, MongoClient, MongoClientSettings, MongoCollection, MongoDatabase}

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
    case request@FindAll(calculateDirections) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(request.toString))
      val ref = sender()
      collection.find()
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
