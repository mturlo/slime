package com.cyberdolphins.slime

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Created by mikwie on 07/01/16.
  */
package object messages {

  case class RtmStartResponse(url: String, ok: Boolean)

  object RtmStartResponse {

    implicit val reads = Json.reads[RtmStartResponse]

  }

  case class RtmStartRequest(token: String)

  object RtmStartRequest {

    implicit val writes = Json.writes[RtmStartRequest]

  }

  object MessageTypes extends Enumeration {

    type MessageType = Value

    val hello   = Value("hello")
    val ping   = Value("ping")
    val pong   = Value("pong")
    val message = Value("message")
    val error   = Value("error")
    val presenceChange = Value("presence_change")
    val userTyping = Value("user_typing")
    val filePublic = Value("file_public")

    @transient
    implicit val writes = new Writes[MessageType] {
      override def writes(o: MessageType): JsValue = {
        JsString(o.toString)
      }
    }

    @transient
    implicit val reads = new Reads[MessageType] {
      override def reads(json: JsValue): JsResult[MessageType] = {
        JsSuccess(MessageTypes.withName(json.as[JsString].value))
      }
    }
  }

  import MessageTypes.MessageType

  sealed trait InboundMessage

  case class Posted(ok: Boolean, replyTo: Option[String], text: String) extends InboundMessage

  object Posted {

    implicit val reads = {
      ((__ \ "ok").read[Boolean] ~
        (__ \ "reply_to").readNullable[String] ~
        (__ \ "text").read[String]) (Posted.apply _)
    }
  }

  sealed class TypedMessage(val `type`: MessageType) extends InboundMessage {
    override def toString: String = s"InboundMessage(${`type`})"
  }

  case object Hello extends TypedMessage(MessageTypes.hello)

  case object Ping extends TypedMessage(MessageTypes.ping)
  case object Pong extends TypedMessage(MessageTypes.pong)

  case class TextMessage(text: String, channel: String, user: String) extends TypedMessage(MessageTypes.message)

  object TextMessage {

    implicit val reads = {
      ((__ \ "text").read[String] ~
        (__ \ "channel").read[String] ~
        (__ \ "user").read[String]) (TextMessage.apply _)
    }
  }

  case class UserTyping(text: String) extends TypedMessage(MessageTypes.userTyping)

  object TypedMessage {

    def apply(`type`: MessageType): TypedMessage = {
      new TypedMessage(`type`)
    }

    def unapply(m: TypedMessage): Option[MessageType] = Some(m.`type`)

    implicit val reads = new Reads[TypedMessage] {
      override def reads(json: JsValue): JsResult[TypedMessage] = {
        (json \ "type").validate[String] match {
          case JsSuccess(name, _) => JsSuccess(TypedMessage(MessageTypes.withName(name)))
          case JsError(e) => JsError(e)
        }
      }
    }
  }

  object InboundMessage {

    implicit val reads = new Reads[InboundMessage] {
      override def reads(json: JsValue): JsResult[InboundMessage] = {
        JsSuccess {
          json.validate[TypedMessage] match {
            case JsSuccess(TypedMessage(MessageTypes.hello), _) => Hello
            case JsSuccess(TypedMessage(MessageTypes.ping), _) => Ping
            case JsSuccess(TypedMessage(MessageTypes.pong), _) => Pong
            case JsSuccess(TypedMessage(MessageTypes.message), _) => json.as[TextMessage]
            case JsSuccess(in@TypedMessage(otherType), _) => in
            case JsError(_) => json.as[Posted]
          }
        }
      }
    }
  }

  case class OutboundMessage(text: String, channel: Option[String], `type`: MessageType = MessageTypes.message)

  object OutboundMessage {

    implicit val writes = {
      ((__ \ "text").write[String] ~
        (__ \ "channel").writeNullable[String] ~
        (__ \ "type").write[MessageType]) (unlift(OutboundMessage.unapply))
    }
  }
}
