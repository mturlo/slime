package com.cyberdolphins.slime

import akka.actor.{ActorSystem, Props}
import com.cyberdolphins.slime.SlackBotActor._
import org.scalatest.WordSpec

/**
  * Created by mwielocha on 07/01/16.
  */
class SlackBotActorSpec extends WordSpec {

  "SlackBotActor" should {

    "connect to Slack rtm" in {

      val actorSystem = ActorSystem()

      class MySlackBot extends SlackBotActor {
        override def eventReceive: EventReceive = {
          case incoming.Message("speak!", channel, user) =>
            publish(outgoing.ComplexOutboundMessage("Spoken!", channel, user,
              outgoing.Attachment("The Expanse", "http://www.imdb.com/title/tt3230854/", "Woohoo!")))
        }
      }

      val mySlackBotActor = actorSystem.actorOf(Props(new MySlackBot))

      mySlackBotActor ! Connect(System.getenv("SLACK_API_KEY"))

      Thread.sleep(50000)

    }
  }
}
