import AndroidServer.CalculateDirections
import Main.CreateInfrastracture
import Master.{RequestTrackMapper, RequestTrackReducer}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Master {
  def props: Props = Props(new Master)
  final case class RequestTrackMapper(str: String)
  final case class RequestTrackReducer(a: String)
}

class Master extends Actor with ActorLogging {
  var mappersGroupActor: ActorRef = _
  var reducersGroupActor: ActorRef = _
  override def preStart(): Unit = log.info("MasterImpl started")
  override def postStop(): Unit = log.info("MasterImpl stopped")
  override def receive: Receive = {
    case CreateInfrastracture =>
      log.info("Creating reducers group actor.")
      reducersGroupActor = context.actorOf(ReducersGroup.props(mappersGroupActor = this.self, masterActor = this.self))
      reducersGroupActor ! RequestTrackReducer("moscow")
      log.info("Creating mappers group actor.")
      mappersGroupActor = context.actorOf(MappersGroup.props(reducersGroupActor = reducersGroupActor, masterActor = this.self))
      context.watch(mappersGroupActor)
      mappersGroupActor ! RequestTrackMapper("havana")
      mappersGroupActor ! RequestTrackMapper("saoPaolo")
      mappersGroupActor ! RequestTrackMapper("athens")
      mappersGroupActor ! RequestTrackMapper("jamaica")
    case request @ CalculateDirections =>
      mappersGroupActor forward request
  }
}

