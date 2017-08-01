import akka.actor.{Actor, ActorLogging}
import com.google.maps.model.DirectionsResult

/**
  * Created by stefanos on 5/5/17.
  */
trait Master extends Actor with ActorLogging {
  def initialize(): Unit

  def waitForNewQueriesThread(): Unit

  def searchCache(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): DirectionsResult

  def distributeToMappers(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): Unit

  def waitForMappers(): Unit

  def ackToReducers(): Unit

  def collectDataFromReducer(): Unit

  def askGoogleDirectionsAPI(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): DirectionsResult

  def updateCache(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint, directions: DirectionsResult): Boolean

  def updateDatabase(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint, directions: DirectionsResult): Boolean
}
