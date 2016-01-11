package com.cyberdolphins.slime

import akka.actor.{ActorSystem, Props}
import com.cyberdolphins.slime.SlackBotActor.{Close, Connect}
import com.cyberdolphins.slime.incoming._
import com.cyberdolphins.slime.outgoing._
import com.cyberdolphins.slime.common._
import MarkdownInValues._

/**
  * Created by mwielocha on 09/01/16.
  */

class Slime extends SlackBotActor {
  override def eventReceive: EventReceive = {

    case SimpleInboundMessage("speak!", channel, user) =>

      val fields = (1 to 10).map(i => Field(s"Field no. *$i*"))

      publish(ComplexOutboundMessage("Spoken!", channel, user,
        Attachment("The Expanse", "http://www.imdb.com/title/tt3230854/", "Woohoo!"),
        Attachment("The Expanse", "http://www.imdb.com/title/tt3230854/", "Woohoo!")
        .withColor(Color.good)
        .withFields(fields: _*)
        .withMarkdownIn(MarkdownInValues.fields)))
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
