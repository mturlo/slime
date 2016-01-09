package com.cyberdolphins.slime.ws

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{ActorSystem, Props}
import com.cyberdolphins.slime.ws.WebSocketActor._
import org.scalatest.WordSpec
import play.api.libs.json.Json

/**
  * Created by mwielocha on 07/01/16.
  */
class WebSocketActorSpec extends WordSpec {

  case class Postcard(body: String)

  "connect" should {

    "blah" in {

      val counter = new AtomicInteger(0)

      implicit val reads = Json.reads[Postcard]
      implicit val writes = Json.writes[Postcard]

      val actorSystem = ActorSystem()

      class MyWebSocketActor extends WebSocketActor[Postcard, Postcard] {

        override def webSocketReceive: WebSocketReceive = {

          case Postcard(text) =>
            println(s"Got: $text")

            sender() ! Send(Postcard(s"Another one: ${counter.incrementAndGet()}"))

        }
      }

      val webSocketActor = actorSystem.actorOf(Props(new MyWebSocketActor))

      webSocketActor ! Open("wss://echo.websocket.org")

      Thread.sleep(2000)

      webSocketActor ! Send(Postcard("Hey!"))

      webSocketActor ! Send(1)

      Thread.sleep(2000)

      webSocketActor ! Close

      Thread.sleep(2000)
    }
  }
}
