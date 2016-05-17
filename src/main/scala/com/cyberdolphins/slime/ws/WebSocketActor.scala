package com.cyberdolphins.slime.ws

import java.net.URI
import javax.net.ssl.SSLContext

import akka.actor._
import com.cyberdolphins.slime.ws.WebSocketActor._
import org.java_websocket.client.{DefaultSSLWebSocketClientFactory, WebSocketClient}
import org.java_websocket.drafts.Draft_10
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json._

import scala.collection.JavaConversions._
import scala.concurrent.duration.{FiniteDuration, _}
import scala.reflect.ClassTag
import java.net.InetSocketAddress


/**
  * Created by mwielocha on 07/01/16.
  */

object WebSocketActor {

  sealed trait WebSocketMessage

  case class Error(ex: Exception) extends WebSocketMessage

  case class Closed(code: Int, reason: String, remote: Boolean) extends WebSocketMessage

  case class Opened(httpStatusCode: Short, httpStatusMessage: String) extends WebSocketMessage

  case class Received(payload: Any) extends WebSocketMessage

  case class Send(message: Any) extends WebSocketMessage

  case class WebSocketConfig(
    serverUri: String,
    headers: Map[String, String],
    connectionTimeout: FiniteDuration,
    proxyHost: Option[String],
    proxyPort: Option[Int])

  object WebSocketConfig {

    def apply(serverUri: String): WebSocketConfig = {
      new WebSocketConfig(serverUri, Map.empty, 1 second, None, None)
    }
  }

  case class Open(config: WebSocketConfig) extends WebSocketMessage

  object Open {

    def apply(serverUri: String): Open = {
      new Open(WebSocketConfig(serverUri))
    }
  }

  case object Close extends WebSocketMessage

  sealed trait WebSocketSate

  case object Uninitialized extends WebSocketSate

  case object Connected extends WebSocketSate

  case object Disconnected extends WebSocketSate

  case object Derailed extends WebSocketSate

  sealed trait WebSocketSateData

  case object NoStateData extends WebSocketSateData

  case class ConnectedStateData(client: WebSocketClient) extends WebSocketSateData

  case class DerailedStateData(ex: Throwable) extends WebSocketSateData
}

abstract class WebSocketActor[In : Reads : ClassTag, Out : Writes : ClassTag] extends FSM[WebSocketSate, WebSocketSateData] with ActorLogging {

  type WebSocketReceive = PartialFunction[In, Any]

  def webSocketReceive: WebSocketReceive

  private val inboundClass = implicitly[ClassTag[In]].runtimeClass
  private val outboundClass = implicitly[ClassTag[Out]].runtimeClass

  startWith(Uninitialized, NoStateData)

  private def connect(config: WebSocketConfig) = {
    log.info(s"Opening connection for $config")
    stay() using ConnectedStateData(new UnderlyingWebSocketClient(config))
  }

  when(Uninitialized) {

    case Event(Open(config), _) => connect(config)

    case Event(Opened(_, _), client) =>
      goto(Connected) using client

    case Event(anything, _) =>
      log.error("Uninitialized!")
      stay()
  }

  when(Connected) {

    case Event(Send(m), ConnectedStateData(client)) if isOutboundValid(m) =>
      log.debug(s"Sending $m")
      send(m.asInstanceOf[Out], client)
      stay()

    case Event(Send(m), ConnectedStateData(client)) =>
      log.warning(s"Outbound message type mismatch, " +
        s"got ${m.getClass.getName} expected ${outboundClass.getName}")
      stay()

    case Event(Received(m), ConnectedStateData(client)) if isInboundValid(m) =>
      webSocketReceive(m.asInstanceOf[In])
      stay()

    case Event(Received(m), _) =>
      log.warning(s"Inbound message type mismatch, " +
        s"got ${m.getClass.getName} expected ${outboundClass.getName}")
      stay()

    case Event(Error(ex), _) =>
      log.error(ex ,"WebSocket just derailed")
      goto(Derailed) using DerailedStateData(ex)

    case Event(Close, ConnectedStateData(client)) =>
      log.debug("Closing connection")
      client.closeBlocking()
      stay()

    case Event(Closed(_, _, _), _) =>
      goto(Disconnected) using NoStateData
  }

  when(Disconnected) {

    case Event(Open(config), _) => connect(config)

    case Event(_, _) =>
      log.warning("WebSocket is disconnected")
      stay()
  }

  when(Derailed) {

    case Event(any, DerailedStateData(ex)) =>
      log.error(ex, "WebSocket is derailed")
      stay()
  }

  private def send(out: Out, client: WebSocketClient) = {
    val payload = Json.toJson(out).toString
    log.debug(s"ws ->: $payload")
    client.send(payload)
  }

  private def isInboundValid(m: Any): Boolean = {
    inboundClass.isAssignableFrom(m.getClass) &&
      webSocketReceive.isDefinedAt(m.asInstanceOf[In])
  }

  private def isOutboundValid(m: Any): Boolean = {
    outboundClass.isAssignableFrom(m.getClass)
  }

  protected def format(payload: String): String = payload

  private class UnderlyingWebSocketClient(config: WebSocketConfig)
    extends WebSocketClient(new URI(config.serverUri), new Draft_10,
      config.headers, config.connectionTimeout.toSeconds.toInt) {

    if(config.serverUri.startsWith("wss")) {
      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(null, null, null)
      setWebSocketFactory(
        new DefaultSSLWebSocketClientFactory(sslContext))
    }

    for {
      proxyHost <- config.proxyHost
      proxyPort <- config.proxyPort
    } {
      log.info(s"Setting up proxy: $proxyHost:$proxyPort")
      setProxy(new InetSocketAddress(proxyHost, proxyPort))
    }

    connectBlocking()

    override def onError(ex: Exception): Unit = {
      self ! Error(ex)
    }

    override def onMessage(message: String): Unit = {

      log.info(s"ws <-: $message")

      Json.parse(message).validate[In] match {
        case JsSuccess(m, _) => self ! Received(m)
        case JsError(_) =>
          log.error(s"Error parsing message: $message")
      }
    }

    override def onClose(code: Int, reason: String, remote: Boolean): Unit = {
      self ! Closed(code, reason, remote = remote)
    }

    override def onOpen(h: ServerHandshake): Unit = {
      self ! Opened(h.getHttpStatus, h.getHttpStatusMessage)
    }
  }
}


