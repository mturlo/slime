package com.cyberdolphins.slime

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

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

    val unknown = Value("unknown")

    val hello          = Value("hello")
    val ping           = Value("ping")
    val pong           = Value("pong")
    val message        = Value("message")
    val error          = Value("error")
    val presenceChange = Value("presence_change")
    val userTyping     = Value("user_typing")
    val filePublic     = Value("file_public")

    val channel_marked = Value
    val channel_created = Value
    val channel_joined = Value
    val channel_left = Value
    val channel_deleted = Value
    val channel_rename = Value
    val channel_archive = Value
    val channel_unarchive = Value
    val channel_history_changed = Value
    val dnd_updated = Value
    val dnd_updated_user = Value
    val im_created = Value
    val im_open = Value
    val im_close = Value
    val im_marked = Value
    val im_history_changed = Value
    val group_joined = Value
    val group_left = Value
    val group_open = Value
    val group_close = Value
    val group_archive = Value
    val group_unarchive = Value
    val group_rename = Value
    val group_marked = Value
    val group_history_changed = Value
    val file_created = Value
    val file_shared = Value
    val file_unshared = Value
    val file_private = Value
    val file_change = Value
    val file_deleted = Value
    val file_comment_added = Value
    val file_comment_edited = Value
    val file_comment_deleted = Value
    val pin_added = Value
    val pin_removed = Value
    val manual_presence_change = Value
    val pref_change = Value
    val user_change = Value
    val team_join = Value
    val star_added = Value
    val star_removed = Value
    val reaction_added = Value
    val reaction_removed = Value
    val emoji_changed = Value
    val commands_changed = Value
    val team_plan_change = Value
    val team_pref_change = Value
    val team_rename = Value
    val team_domain_change = Value
    val email_domain_changed = Value
    val team_profile_change = Value
    val team_profile_delete = Value
    val team_profile_reorder = Value
    val bot_added = Value
    val bot_changed = Value
    val accounts_changed = Value
    val team_migration_started = Value
    val subteam_created = Value
    val subteam_updated = Value
    val subteam_self_added = Value
    val subteam_self_removed = Value

    @transient
    implicit val writes = new Writes[EventType] {
      override def writes(o: EventType): JsValue = {
        JsString(o.toString)
      }
    }

    @transient
    implicit val reads = new Reads[EventType] {
      override def reads(json: JsValue): JsResult[EventType] = {
        JsSuccess {
          Try(EventTypes.withName(
            json.as[JsString].value))
            .getOrElse(unknown)
        }
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
