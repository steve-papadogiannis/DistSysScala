package gr.papadogiannis.stefanos.repos

import akka.actor.{Actor, ActorLogging}
import gr.papadogiannis.stefanos.messages.{DBResult, FindAll}
import gr.papadogiannis.stefanos.models._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.{ConnectionString, MongoClient, MongoClientSettings, MongoDatabase}

class MongoActor extends Actor with ActorLogging {

  override def preStart(): Unit = log.info("MongoActor started")

  override def postStop(): Unit = log.info("MongoActor stopped")

  val codecRegistries = getCodecRegistries
  val mongoClientSettings = getMongoClientSettings(codecRegistries)
  val mongoClient = getMongoClient(mongoClientSettings)
  val database = getMongoDatabase(mongoClient)
  val collection = getMongoCollection(database)

  override def receive: Receive = {
    case FindAll(calculateDirections) =>
      collection.find[DirectionsResultWrapper]()
        .collect()
        .subscribe(finalResult => {
          sender() ! DBResult(calculateDirections, finalResult.toList)
        }, throwable => {
          log.error(throwable.toString)
          log.error(throwable.getStackTrace.mkString("\n"))
        })
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
