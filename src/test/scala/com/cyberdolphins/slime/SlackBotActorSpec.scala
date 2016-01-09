package com.cyberdolphins.slime

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Props, Actor, ActorSystem}
import com.cyberdolphins.slime.ws.WebSocketActor
import com.cyberdolphins.slime.ws.WebSocketActor.{Open, Send, Received, Opened}
import org.scalatest.WordSpec
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import SlackBotActor._
import incoming._

/**
  * Created by mwielocha on 07/01/16.
  */
class SlackBotActorSpec extends WordSpec {

  "SlackBotActor" should {

    "connect to Slack rtm" in {

      val actorSystem = ActorSystem()

      class MySlackBot extends SlackBotActor {
        override def eventReceive: EventReceive = {
          case Message("speak!", channel, user) =>
            publish(outgoing.Message("Spoken!", channel, user))
        }
      }

      val mySlackBotActor = actorSystem.actorOf(Props(new MySlackBot))

      mySlackBotActor ! Connect(System.getenv("SLACK_API_KEY"))

      Thread.sleep(50000)

    }
  }
}
