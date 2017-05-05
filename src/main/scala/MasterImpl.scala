import com.google.maps.model.DirectionsResult
/**
  * Created by stefanos on 5/5/17.
  */
class MasterImpl extends Master{
  override def initialize() {
    val mapWorker = new MapWorker()
    mapWorker
  }

  override def waitForNewQueriesThread(): Unit = ???

  override def searchCache(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): DirectionsResult = ???

  override def distributeToMappers(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): Unit = ???

  override def waitForMappers(): Unit = ???

  override def ackToReducers(): Unit = ???

  override def collectDataFromReducer(): Unit = ???

  override def askGoogleDirectionsAPI(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): DirectionsResult = ???

  override def updateCache(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint, directions: DirectionsResult): Boolean = ???

  override def updateDatabase(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint, directions: DirectionsResult): Boolean = ???
}
