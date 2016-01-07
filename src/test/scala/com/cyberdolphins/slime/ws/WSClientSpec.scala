package com.cyberdolphins.slime.ws

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorSystem}
import com.cyberdolphins.slime.ws.WSClient.Opened
import org.scalatest.WordSpec
import play.api.libs.json.{Json, JsValue}

/**
  * Created by mikwie on 07/01/16.
  */
class WSClientSpec extends WordSpec {

  case class Postcard(body: String)

//  "WSClient" should {
//
//    "blah" in {
//
//      implicit val reads = Json.reads[Postcard]
//      implicit val writes = Json.writes[Postcard]
//
//      val actorSystem = ActorSystem()
//
//      class Receiver extends Actor {
//        override def receive: Actor.Receive = {
//          case Message(Postcard(text)) =>
//            println(s"Got: $text")
//
//          case o: Opened =>
//            println(s"Opened: $o")
//
//            sender() ! Send(Postcard("Woohoo!"))
//        }
//      }
//
//      Thread.sleep(1000)
//    }
//  }
}
