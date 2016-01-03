package org.panda.eventsourcing.game

import akka.actor._
import scala.collection.mutable.ListBuffer

case class UpdateParameters(id: Int = -1, homeWinProbability: Double)

case class ParametersUpdated(parameters: UpdateParameters)
case class ParametersStored(parameters: UpdateParameters)
case class ProbabilitiesUpdated(parameters: UpdateParameters, eventId: Long)

case class ProbabilityUpdateRequested(requestId: Int, homeWinProbability: Double)
case class ProbabilitiesReceived(requestId: Int, outcomeProbabilities: List[Double])
case class ProbabilityServiceFailure(requestId: Int)


class Game(probabilityService: ActorRef) extends Actor {

  var events = new ListBuffer[Object]()
  def apply(event: Object) { events += event }


  def receive = {
    case message : ParametersUpdated => {
      val parameters = message.parameters
      println(s"command received $parameters")
      apply(message)

      context.actorOf(Props(new Actor() {
        probabilityService ! ProbabilityUpdateRequested(parameters.id, parameters.homeWinProbability)

        def receive = {

          case message : ProbabilitiesReceived => {
            val id = message.requestId
            val probabilities = message.outcomeProbabilities
            println(s"message id $id, probabilities $probabilities" )
            apply(message)
          }

          case message : ProbabilityServiceFailure => {
            println("remote service failed!")
            apply(message)
          }

        }
      })) // end of context

    }
  }

}

class ProbabilityService extends Actor {

  var failReply = false

  def calculateTheOdds(homeWinProbability: Double): List[Double] = {
    Thread.sleep(1000) // to simulate the processing....
    (1/homeWinProbability) :: (1/(1-homeWinProbability)) :: Nil
  }

  def receive = {
    case ProbabilityUpdateRequested(commandId, homeWinProbability) => {
      val odds = calculateTheOdds(homeWinProbability)

      if (failReply)
        sender ! ProbabilityServiceFailure(commandId)
      else
        sender ! ProbabilitiesReceived(commandId, odds)

      failReply ^= true
    }
  }
}

object GameStateExample extends App {
  val system = ActorSystem("GameProbabilityServiceSystem")
  val probabilityService = system.actorOf(Props[ProbabilityService], name = "probabilityService")
  val game = system.actorOf(Props(new Game(probabilityService)), name = "game")

  game ! ParametersUpdated(new UpdateParameters(1, 0.24))
  game ! ParametersUpdated(new UpdateParameters(2, 0.54))
}