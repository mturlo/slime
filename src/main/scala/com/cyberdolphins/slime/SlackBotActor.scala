package com.cyberdolphins.slime

import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.actor.Actor.Receive
import com.cyberdolphins.slime.incoming._
import com.cyberdolphins.slime.ws.WebSocketActor
import com.cyberdolphins.slime.ws.WebSocketActor.WebSocketConfig
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_10
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json._
import scala.collection.JavaConversions._
import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

import SlackBotActor._
import outgoing._

import scala.util.{Failure, Success}

/**
  * Created by mwielocha on 07/01/16.
  */


object SlackBotActor {

  sealed trait SlackBotActorState
  case object Connected extends SlackBotActorState
  case object Disconnected extends SlackBotActorState

  sealed trait SlackBotActorStateData
  case object NoStateData extends SlackBotActorStateData
  case class Token(token: String) extends SlackBotActorStateData

  sealed trait SlackBotActorMessage

  type LazyWebSocketConfig = String => WebSocketConfig

  case class Connect(token: String, config: LazyWebSocketConfig = WebSocketConfig(_)) extends SlackBotActorMessage
  case object Close extends SlackBotActorMessage

}

abstract class SlackBotActor extends FSM[SlackBotActorState, SlackBotActorStateData] with ActorLogging {

  startWith(Disconnected, NoStateData)

  import context.dispatcher

  type EventReceive = PartialFunction[incoming.Event, Unit]

  def eventReceive: EventReceive

  private val rtmStartUrl = "https://slack.com/api/rtm.start?token=%s"
  private val chatPostMessageUrl = "https://slack.com/api/chat.postMessage"

  private class SlackWebSocketActor extends WebSocketActor[Response, Outbound] {
    override def webSocketReceive: WebSocketReceive = {
      case m => SlackBotActor.this.self ! m
    }
  }

  private val counter = new AtomicInteger(0)
  private def nextID: Int = counter.incrementAndGet()

  private val webSocketActor = context.actorOf(Props(new SlackWebSocketActor))

  private val httpClientBuilder = new AsyncHttpClientConfig.Builder()
  private val httpClient = new NingWSClient(httpClientBuilder.build())

  private def connect(token: String, config: String => WebSocketConfig) = {

    httpClient.url(rtmStartUrl.format(token)).get().map { response =>
      response.json.validate[RtmStartResponse] match {

        case JsSuccess(RtmStartResponse(wsUrl, true), _) =>
          log.info("Connecting to slack...")
          webSocketActor ! WebSocketActor.Open(config(wsUrl))

        case JsError(_) =>
          log.error("Unexpected response from rtm")
      }
    }
  }

  private def close() = {
    webSocketActor ! WebSocketActor.Close
  }

  private def httpPost(m: outgoing.Message, token: String): Future[Response] = {

    log.info(s"httpPost: $m")

    val request = httpClient.url(chatPostMessageUrl)

    val futureResponse = request.post(Map(
      "token"   -> Seq(token),
      "channel" -> Seq(m.channel),
      "text"    -> Seq(m.text),
      "as_user" -> Seq("true")
    ))

    futureResponse.onComplete {
      case Success(response) if response.status == 200 => log.info(response.body)
      case Success(response) => log.warning(s"Non 200 response status: ${response.body}")
      case Failure(ex) => log.error(ex, "Error on posting message")
    }

    futureResponse.map {
      response => response.json.as[Response]
    }
  }

  private def webSocketPush(o: Outbound) = {

    log.info(s"webSocketPush: $o")

    webSocketActor ! WebSocketActor.Send {
      o.id match {
        case None => o.stamp(nextID)
        case Some(_) => o
      }
    }
  }

  private def publishAsync(message: Future[Outbound], token: String) = {
    message.foreach {
      case o: outgoing.Message => httpPost(o, token)
      case o: Outbound => webSocketPush(o)
    }
  }

  private def publish(message: Outbound, token: String) = {
    publishAsync(Future.successful(message), token)
  }

  def publishAsync(message: Future[Outbound]) = {
    message.foreach(self ! _)
  }

  def publish(message: Outbound) = {
    publishAsync(Future.successful(message))
  }

  when(Disconnected) {

    case Event(SlackBotActor.Connect(token, config), _) =>
      connect(token, config)
      goto(Connected) using Token(token)
  }

  when(Connected) {

    case Event(SlackBotActor.Close, _) =>
      close()
      goto(Disconnected) using NoStateData

    case Event(m: Outbound, Token(token)) => publish(m, token); stay()

    case Event(m: incoming.Event, _) if eventReceive.isDefinedAt(m) => eventReceive(m); stay()
  }
}