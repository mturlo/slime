package com.cyberdolphins.slime

import akka.actor.{Props, ActorSystem}
import com.cyberdolphins.slime.SlackBotActor.{Close, Connect}
import com.cyberdolphins.slime.outgoing.ComplexOutboundMessage
import incoming._
import outgoing._

/**
  * Created by mwielocha on 09/01/16.
  */

class Slime extends SlackBotActor {
  override def eventReceive: EventReceive = {

    case Message("speak!", channel, user) =>

      publish(ComplexOutboundMessage("Spoken!", channel, user,
        Attachment("The Expanse", "http://www.imdb.com/title/tt3230854/", "Woohoo!")))
  }
}


object Slime extends App {

  val actorSystem = ActorSystem()

  val mySlackBotActor = actorSystem.actorOf(Props[Slime], "slime-bot")

  mySlackBotActor ! Connect(System.getenv("SLACK_API_KEY"))

  sys.addShutdownHook {
    mySlackBotActor ! Close
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }

  actorSystem.awaitTermination()
}
