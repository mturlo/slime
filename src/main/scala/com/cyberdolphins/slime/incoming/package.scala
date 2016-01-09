package com.cyberdolphins.slime

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Created by mwielocha on 07/01/16.
  */
package object incoming {

  case class RtmStartResponse(ok: Boolean, url: Option[String])

  object RtmStartResponse {

    implicit val reads = Json.reads[RtmStartResponse]

  }

  case class RtmStartRequest(token: String)

  object RtmStartRequest {

    implicit val writes = Json.writes[RtmStartRequest]

  }

  object EventTypes extends Enumeration {

    type EventType = Value

    val hello          = Value("hello")
    val ping           = Value("ping")
    val pong           = Value("pong")
    val message        = Value("message")
    val error          = Value("error")
    val presenceChange = Value("presence_change")
    val userTyping     = Value("user_typing")
    val filePublic     = Value("file_public")

    @transient
    implicit val writes = new Writes[EventType] {
      override def writes(o: EventType): JsValue = {
        JsString(o.toString)
      }
    }

    @transient
    implicit val reads = new Reads[EventType] {
      override def reads(json: JsValue): JsResult[EventType] = {
        JsSuccess(EventTypes.withName(json.as[JsString].value))
      }
    }
  }

  import EventTypes.EventType

  sealed trait Inbound

  sealed trait Response

  sealed class Event extends Response with Inbound

  //"error":{"code":1,"msg":"no channel id"}

  case class Error(code: Int, msg: String)

  object Error {
    implicit val reads = Json.reads[Error]
  }

  case class Posted(ok: Boolean, replyTo: Option[Int], text: Option[String], error: Option[Error])
    extends Response with Inbound

  object Posted {

    implicit val reads = {
      ((__ \ "ok").read[Boolean] ~
        (__ \ "reply_to").readNullable[Int] ~
        (__ \ "text").readNullable[String] ~
        (__ \ "error").readNullable[Error]) (Posted.apply _)
    }
  }

  sealed class TypedEvent(val `type`: EventType) extends Event {
    override def toString: String = s"Event(${`type`})"
  }

  case object Hello extends TypedEvent(EventTypes.hello)

  case object Pong extends TypedEvent(EventTypes.pong)

  case class Message(
    text: String,
    channel: String,
    user: String) extends TypedEvent(EventTypes.message)

  object Message {

    implicit val reads = {
      ((__ \ "text").read[String] ~
        (__ \ "channel").read[String] ~
        (__ \ "user").read[String]) (Message.apply _)
    }
  }

  case class UserTyping(text: String) extends TypedEvent(EventTypes.userTyping)

  object TypedEvent {

    def apply(`type`: EventType): TypedEvent = {
      new TypedEvent(`type`)
    }

    def unapply(m: TypedEvent): Option[EventType] = Some(m.`type`)

    implicit val reads = new Reads[TypedEvent] {
      override def reads(json: JsValue): JsResult[TypedEvent] = {
        (json \ "type").validate[String] match {
          case JsSuccess(name, _) => JsSuccess(TypedEvent(EventTypes.withName(name)))
          case JsError(e) => JsError(e)
        }
      }
    }
  }

  object Response {

    implicit val reads = new Reads[Response] {
      override def reads(json: JsValue): JsResult[Response] = {
        JsSuccess {
          json.validate[TypedEvent] match {
            case JsSuccess(TypedEvent(EventTypes.hello), _) => Hello
            case JsSuccess(TypedEvent(EventTypes.pong), _) => Pong
            case JsSuccess(TypedEvent(EventTypes.message), _) => json.as[Message]
            case JsSuccess(in@TypedEvent(otherType), _) => in
            case JsError(_) => json.as[Posted]
          }
        }
      }
    }
  }
}
