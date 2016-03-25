package com.cyberdolphins.slime

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import com.cyberdolphins.slime.SlackBotActor._
import com.cyberdolphins.slime.common.{User, Channel}
import com.cyberdolphins.slime.common.Strings._
import com.cyberdolphins.slime.incoming._
import com.cyberdolphins.slime.outgoing._
import com.cyberdolphins.slime.ws.WebSocketActor
import com.cyberdolphins.slime.ws.WebSocketActor.WebSocketConfig
import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.json._
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.Future
import scala.concurrent.duration._
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
  case class ConnectedState(token: String, config: SlackBotConfig)
    extends SlackBotActorStateData

  case class SlackBotConfig(autoEscape: Boolean = true)

  sealed trait SlackBotActorMessage

  type LazyWebSocketConfig = String => WebSocketConfig

  case class Connect(
    token: String,
    slackBotConfig: SlackBotConfig = SlackBotConfig(autoEscape = true),
    webSocketConfig: LazyWebSocketConfig = WebSocketConfig(_)
  )
    extends SlackBotActorMessage

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

    override protected def format(payload: String): String = {
      SlackBotActor.this.format(super.format(payload))
    }

    onTransition {
      case _ -> WebSocketActor.Connected => setupHeartbeat()
    }
  }

  private val counter = new AtomicInteger(0)
  private def nextID: Int = counter.incrementAndGet()

  private val webSocketActor = context.actorOf(Props(new SlackWebSocketActor), "websocket-actor")

  private val httpClientBuilder = new AsyncHttpClientConfig.Builder()
  private val httpClient = new NingWSClient(httpClientBuilder.build())

  private def setupHeartbeat(): Unit = {
    context.system.scheduler.schedule(5 seconds, 5 seconds) {
      self ! Ping(None)
    }
  }

  private def connect(token: String, config: String => WebSocketConfig) = {

    httpClient.url(rtmStartUrl.format(token)).get().map { response =>
      response.json.validate[RtmStartResponse] match {

        case JsSuccess(RtmStartResponse(true, Some(url)), _) =>
          log.debug("Connecting to slack...")
          webSocketActor ! WebSocketActor.Open(config(url))

        case JsSuccess(RtmStartResponse(false, None), _) =>
          log.error("Error starting rtm")

        case JsError(_) =>
          log.error("Unexpected response from rtm")
      }
    }
  }

  private def close() = {
    webSocketActor ! WebSocketActor.Close
  }

  private def format(in: String): String = {
    stateData match {
      case ConnectedState(_, config) if config.autoEscape => in.escape
      case _ => in
    }
  }

  private def httpPost(m: outgoing.ComplexOutboundMessage, token: String): Future[Response] = {

    log.debug(s"httpPost: $m")

    val request = httpClient.url(chatPostMessageUrl)

    val attachments = format {
      Json.toJson(m.attachments).toString()
    }

    val futureResponse = request.post(Map(
      "token" -> Seq(token),
      "text" -> Seq(m.text),
      "as_user" -> Seq("true"),
      "attachments" -> Seq(attachments),
      "channel" -> Seq(m.channel.name)
    ))

    futureResponse.onComplete {
      case Success(response) if response.status == 200 => log.debug(response.body)
      case Success(response) => log.warning(s"Non 200 response status: ${response.body}")
      case Failure(ex) => log.error(ex, "Error on posting message")
    }

    futureResponse.map {
      response => response.json.as[Response]
    }
  }

  private def webSocketPush(o: Outbound) = {

    log.debug(s"webSocketPush: $o")

    webSocketActor ! WebSocketActor.Send {
      o.id match {
        case None => o.stamp(nextID)
        case Some(_) => o
      }
    }
  }

  private def publishAsync(message: Future[Outbound], token: String) = {
    message.foreach {
      case o: outgoing.ComplexOutboundMessage => httpPost(o, token)
      case o: Outbound => webSocketPush(o)
    }
  }

  private def publish(message: Outbound, token: String) = {
    publishAsync(Future.successful(message), token)
  }

  def publishAsync(message: Future[Outbound]) = {
    message.foreach(self ! _)
  }

  def publish(message: Outbound): Unit = {
    publishAsync(Future.successful(message))
  }

  def publish(text: String, channel: Channel, user: User): Unit = {
    publish(SimpleOutboundMessage(text, channel, user))
  }

  when(Disconnected) {

    case Event(SlackBotActor.Connect(token, slackBotConfig, webSocketConfig), _) =>
      connect(token, webSocketConfig)
      goto(Connected) using ConnectedState(token, slackBotConfig)
  }

  when(Connected) {

    case Event(SlackBotActor.Close, _) =>
      close()
      goto(Disconnected) using NoStateData

    case Event(m: Outbound, ConnectedState(token, slackBotConfig)) =>
      publish(m, token); stay()

    case Event(Pong, _) => stay()

    case Event(m: incoming.Event, _) if eventReceive.isDefinedAt(m) => eventReceive(m); stay()
  }
}

class OutgoingOnlySlackBotActor extends SlackBotActor {
  override def eventReceive: EventReceive = {
    case _ => Unit
  }
}
