import akka.actor.{Actor, ActorLogging, Props}

object Master {
  def props: Props = Props(new Master)
}

class Master extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("MasterImpl started")
  override def postStop(): Unit = log.info("MasterImpl stopped")
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
