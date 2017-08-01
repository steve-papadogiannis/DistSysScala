import akka.actor.Props
import com.google.maps.model.DirectionsResult
/**
  * Created by stefanos on 5/5/17.
  */
object MasterImpl {
  def props: Props = Props(new MasterImpl)
}

class MasterImpl extends Master{
  override def initialize() {
    val mapWorker = new MapWorker()
    mapWorker
  }

  override def preStart(): Unit = log.info("MasterImpl started")
  override def postStop(): Unit = log.info("MasterImpl stopped")

  override def waitForNewQueriesThread(): Unit = ???

  override def searchCache(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): DirectionsResult = ???

  override def distributeToMappers(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): Unit = ???

  override def waitForMappers(): Unit = ???

  override def ackToReducers(): Unit = ???

  override def collectDataFromReducer(): Unit = ???

  override def askGoogleDirectionsAPI(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint): DirectionsResult = ???

  override def updateCache(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint, directions: DirectionsResult): Boolean = ???

  override def updateDatabase(startGeoPoint: GeoPoint, endGeoPoint: GeoPoint, directions: DirectionsResult): Boolean = ???

  override def receive: Receive = {
    case CreateInfrastracture =>
      log.info("Creating mappers group actor.")
      val mappersGroupActor = context.actorOf(MappersGroup.props)
      context.watch(mappersGroupActor)
      mappersGroupActor ! RequestTrackReducer("havana")
      mappersGroupActor ! RequestTrackReducer("saoPaolo")
      mappersGroupActor ! RequestTrackReducer("athens")
      mappersGroupActor ! RequestTrackReducer("jamaica")
      log.info("Creating reducers group actor.")
      val reducersGroupActor = context.actorOf(ReducersGroup.props)
      reducersGroupActor ! RequestTrackReducer("moscow")
  }
}
