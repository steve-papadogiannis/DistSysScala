package gr.papadogiannis.stefanos.caches

import gr.papadogiannis.stefanos.constants.ApplicationConstants.RECEIVED_MESSAGE_PATTERN
import gr.papadogiannis.stefanos.models.{DirectionsResult, GeoPointPair}
import gr.papadogiannis.stefanos.messages.{CacheCheck, UpdateCache}
import akka.actor.{Actor, ActorLogging, Props}

object MemCache {
  def props(): Props = Props(new MemCache)
}

class MemCache extends Actor with ActorLogging {

  var cache: Map[GeoPointPair, DirectionsResult] = Map.empty[GeoPointPair, DirectionsResult]

  override def preStart(): Unit = log.info("MemCache started")

  override def postStop(): Unit = log.info("MemCache stopped")

  override def receive: Receive = {
    case message@CacheCheck(geoPointPair) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      val directionsResultOption = cache.get(geoPointPair)
      sender() ! directionsResultOption
    case message@UpdateCache(geoPointPair, directionsResult) =>
      log.info(RECEIVED_MESSAGE_PATTERN.format(message.toString))
      cache + (geoPointPair -> directionsResult)
  }

}
