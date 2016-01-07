package com.cyberdolphins.slime.ws

import akka.actor.{Props, ActorRef, ActorSystem}
import play.api.libs.json.{Writes, Reads}

import scala.concurrent.duration._


/**
  * Created by mikwie on 07/01/16.
  */

object WSClientBuilder {

  def apply[In : Reads, Out : Writes](actorSystem: ActorSystem, serverUri: String): WSClientBuilder[In, Out] = {
    new WSClientBuilder[In, Out](actorSystem, serverUri, Map.empty, 100 millis, Nil)
  }

  def apply[In : Reads, Out : Writes](actorSystem: ActorSystem, serverUri: String, receiver: Props): WSClientBuilder[In, Out] = {
    new WSClientBuilder[In, Out](actorSystem, serverUri, Map.empty, 100 millis, Seq(actorSystem.actorOf(receiver)))
  }
}

case class WSClientBuilder[In : Reads, Out : Writes](actorSystem: ActorSystem,
                                                     serverUri: String, headers: Map[String, String],
                                                     timeout: FiniteDuration, receivers: Seq[ActorRef]) {

  def withReceivers(receivers: ActorRef*): WSClientBuilder[In, Out] = {
    copy(receivers = receivers.toSeq)
  }

  def withHeaders(headers: Map[String, String]): WSClientBuilder[In, Out] = {
    copy(headers = headers)
  }

  def build(): WSClient[In, Out] = {
    new WSClient[In, Out](actorSystem, serverUri, headers, timeout, receivers: _*)
  }
}
