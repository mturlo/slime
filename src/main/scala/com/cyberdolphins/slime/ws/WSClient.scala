package com.cyberdolphins.slime.ws

import java.net.URI
import javax.net.ssl.SSLContext

import akka.actor.Actor.Receive
import akka.actor._
import akka.io.Tcp.Write
import org.java_websocket.client.{DefaultSSLWebSocketClientFactory, WebSocketClient}
import org.java_websocket.drafts.Draft_10
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json.{Writes, Json, Reads}

import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.collection.JavaConversions._
import WSClient._

import scala.reflect.ClassTag

/**
  * Created by mikwie on 07/01/16.
  */

object WSClient {

  sealed trait InternalMessage

  case class Error(ex: Exception) extends InternalMessage

  case class Closed(code: Int, reason: String, remote: Boolean) extends InternalMessage

  case class Opened(httpStatus: Short, httpStatusMessage: String) extends InternalMessage

  case class Received[M](payload: M) extends InternalMessage

  case class Send[M](message: M) extends InternalMessage

}

class WSClient[In : Reads, Out : Writes](actorSystem: ActorSystem,
                                        serverUri: String, headers: Map[String, String],
                                        timeout: FiniteDuration, receivers: ActorRef*) {

  val actor = actorSystem.actorOf(Props(new AkkaProxy))

  private lazy val webSocketClient = new WebSocketClient(new URI(serverUri), new Draft_10, headers, timeout.toSeconds.toInt) {

    override def onError(ex: Exception): Unit = {
      ex.printStackTrace()
      actor ! Error(ex)
    }

    override def onMessage(message: String): Unit = {
      println(s"Raw: $message")
      actor ! Received(Json.parse(message).as[In])
    }

    override def onClose(code: Int, reason: String, remote: Boolean): Unit = {
      actor ! Closed(code, reason, remote = remote)
    }

    override def onOpen(h: ServerHandshake): Unit = {
      actor ! Opened(h.getHttpStatus, h.getHttpStatusMessage)
    }
  }

  if(serverUri.startsWith("wss")) {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, null, null)
    webSocketClient.setWebSocketFactory(
      new DefaultSSLWebSocketClientFactory(sslContext))
  }

  private class AkkaProxy extends Actor with ActorLogging {
    override def receive: Receive = {

      case s@Send(p) =>

        log.info(s"Sending: $s")

        webSocketClient.send(Json.toJson(
          p.asInstanceOf[Out]).toString())

      case im: InternalMessage =>
        log.info(s"Got message: $im")
        receivers.foreach(_ ! im)

    }
  }

  def connect(): WSClient[In, Out] = {
    webSocketClient.connectBlocking()
    this
  }

  def shutdown(): Unit = {
    webSocketClient.closeBlocking()
  }
}


