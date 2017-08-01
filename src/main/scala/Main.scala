import akka.actor.ActorSystem

import scala.io.StdIn

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("DirectionsResultMapReduceSystem")
    try {
      val supervisor = system.actorOf(Supervisor.props(), "supervisor")
      supervisor ! CreateInfrastracture
      println(">>> Press ENTER to exit <<<")
      StdIn.readLine()
    } finally {
      system.terminate()
    }
  }
}
