import Main.CreateInfrastracture
import Master.{RequestTrackMapper, RequestTrackReducer}
import akka.actor.{Actor, ActorLogging, Props}

object Master {
  def props: Props = Props(new Master)
  final case class RequestTrackMapper(str: String)
  final case class RequestTrackReducer(a: String)
}

class Master extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("MasterImpl started")
  override def postStop(): Unit = log.info("MasterImpl stopped")
  override def receive: Receive = {
    case CreateInfrastracture =>
      log.info("Creating mappers group actor.")
      val mappersGroupActor = context.actorOf(ReducersGroup.props)
      context.watch(mappersGroupActor)
      mappersGroupActor ! RequestTrackMapper("havana")
      mappersGroupActor ! RequestTrackMapper("saoPaolo")
      mappersGroupActor ! RequestTrackMapper("athens")
      mappersGroupActor ! RequestTrackMapper("jamaica")
      log.info("Creating reducers group actor.")
      val reducersGroupActor = context.actorOf(ReducersGroup.props)
      reducersGroupActor ! RequestTrackReducer("moscow")
  }
}

