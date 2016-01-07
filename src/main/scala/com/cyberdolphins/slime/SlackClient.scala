package com.cyberdolphins.slime

import java.net.URI

import akka.actor.{Props, Actor, ActorSystem}
import akka.actor.Actor.Receive
import com.cyberdolphins.slime.messages.RtmStartResponse
import com.cyberdolphins.slime.ws.WSClient.Send
import com.cyberdolphins.slime.ws.WSClientBuilder
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_10
import org.java_websocket.handshake.ServerHandshake
import scala.collection.JavaConversions._
import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.ning.NingWSClient
import com.cyberdolphins.slime.messages._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Created by mikwie on 07/01/16.
  */

class SlackClient(private val actorSystem: ActorSystem, private val apiToken: String, receiver: Props)(implicit val ec: ExecutionContext) {

  private val httpClientBuilder = new AsyncHttpClientConfig.Builder()
  private val httpClient = new NingWSClient(httpClientBuilder.build())

  def connect(): Future[Unit] = {
    httpClient.url(s"https://slack.com/api/rtm.start?token=$apiToken").get().map {
      response =>

        println(response.body)

        response.json.as[RtmStartResponse] match {
        case RtmStartResponse(wsUri, false) =>
          throw new RuntimeException("Cannot connect to rtm!")

        case resp@RtmStartResponse(wsUri, true) =>

          println(resp)

          val ws = WSClientBuilder[InboundMessage, OutboundMessage](actorSystem,
            wsUri, receiver).build().connect()

          actorSystem.scheduler.schedule(5 seconds, 5 seconds, ws.actor, Send(Ping))

          sys.addShutdownHook {
            ws.shutdown()
            httpClient.close()
          }
      }
    }
  }
}