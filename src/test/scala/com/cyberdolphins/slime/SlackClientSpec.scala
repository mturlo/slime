package com.cyberdolphins.slime

import java.util.concurrent.Executors

import akka.actor.{Props, Actor, ActorSystem}
import com.cyberdolphins.slime.messages.{OutboundMessage, TextMessage}
import com.cyberdolphins.slime.ws.WSClient.{Send, Received, Opened}
import org.scalatest.WordSpec

import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}

/**
  * Created by mikwie on 07/01/16.
  */
class SlackClientSpec extends WordSpec {

  "SlackClient" should {

    "connect" in {

      val actorSystem = ActorSystem()

      implicit val ec = ExecutionContext.fromExecutor(
        Executors.newFixedThreadPool(10))

      class Receiver extends Actor {
        override def receive: Actor.Receive = {

          case Received(TextMessage("speak!", channel, user)) =>
            sender() ! Send(OutboundMessage("Spoken!", Some(channel)))

          case Received(TextMessage(text, channel, user)) =>
            println(s"Got: $text")

          case o: Opened =>
            println(s"Opened: $o")
        }
      }

      val future = new SlackClient(actorSystem,
        System.getenv("SLACK_API_KEY"),
        Props(new Receiver)
      ).connect()

      future.onComplete {
        case Success(_) => println("Ok")
        case Failure(ex) => ex.printStackTrace()
      }

      Thread.sleep(100000)

    }
  }
}
