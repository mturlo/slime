package com.cyberdolphins.slime

import akka.actor.{ActorSystem, Props}
import com.cyberdolphins.slime.SlackBotActor.{Close, Connect}
import com.cyberdolphins.slime.incoming._
import com.cyberdolphins.slime.outgoing._

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

  val slimeBotActor = actorSystem.actorOf(Props[Slime], "slime-bot")

  slimeBotActor ! Connect(System.getenv("SLACK_API_KEY"))

  sys.addShutdownHook {
    slimeBotActor ! Close
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }

  actorSystem.awaitTermination()
}
