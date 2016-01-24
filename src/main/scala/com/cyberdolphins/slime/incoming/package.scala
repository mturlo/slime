package com.cyberdolphins.slime

import play.api.libs.functional.syntax._
import play.api.libs.json._
import common._

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

    val hello                = Value("hello")
    val ping                 = Value("ping")
    val pong                 = Value("pong")
    val message              = Value("message")
    val userTyping           = Value("user_typing")
    val channelMarked        = Value("channel_marked")
    val channelCreated       = Value("channel_created")
    val channelJoined        = Value("channel_joined")
    val channelLeft          = Value("channel_left")
    val channelDeleted       = Value("channel_deleted")
    val channelRename        = Value("channel_rename")
    val channelArchive       = Value("channel_archive")
    val channelUnarchive     = Value("channel_unarchive")
    val channelHistoryChanged= Value("channel_history_changed")
    val dndUpdated           = Value("dnd_updated")
    val dndUpdatedUser       = Value("dnd_updated_user")
    val imCreated            = Value("im_created")
    val imOpen               = Value("im_open")
    val imClose              = Value("im_close")
    val imMarked             = Value("im_marked")
    val imHistoryChanged     = Value("im_history_changed")
    val groupJoined          = Value("group_joined")
    val groupLeft            = Value("group_left")
    val groupOpen            = Value("group_open")
    val groupClose           = Value("group_close")
    val groupArchive         = Value("group_archive")
    val groupUnarchive       = Value("group_unarchive")
    val groupRename          = Value("group_rename")
    val groupMarked          = Value("group_marked")
    val groupHistoryChanged  = Value("group_history_changed")
    val fileCreated          = Value("file_created")
    val fileShared           = Value("file_shared")
    val fileUnshared         = Value("file_unshared")
    val filePublic           = Value("file_public")
    val filePrivate          = Value("file_private")
    val fileChange           = Value("file_change")
    val fileDeleted          = Value("file_deleted")
    val fileCommentAdded     = Value("file_comment_added")
    val fileCommentEdited    = Value("file_comment_edited")
    val fileCommentDeleted   = Value("file_comment_deleted")
    val pinAdded             = Value("pin_added")
    val pinRemoved           = Value("pin_removed")
    val presenceChange       = Value("presence_change")
    val manualPresenceChange = Value("manual_presence_change")
    val prefChange           = Value("pref_change")
    val userChange           = Value("user_change")
    val teamJoin             = Value("team_join")
    val starAdded            = Value("star_added")
    val starRemoved          = Value("star_removed")
    val reactionAdded        = Value("reaction_added")
    val reactionRemoved      = Value("reaction_removed")
    val emojiChanged         = Value("emoji_changed")
    val commandsChanged      = Value("commands_changed")
    val teamPlanChange       = Value("team_plan_change")
    val teamPrefChange       = Value("team_pref_change")
    val teamRename           = Value("team_rename")
    val teamDomainChange     = Value("team_domain_change")
    val emailDomainChanged   = Value("email_domain_changed")
    val teamProfileChange    = Value("team_profile_change")
    val teamProfileDelete    = Value("team_profile_delete")
    val teamProfileReorder   = Value("team_profile_reorder")
    val botAdded             = Value("bot_added")
    val botChanged           = Value("bot_changed")
    val accountsChanged      = Value("accounts_changed")
    val teamMigrationStarted = Value("team_migration_started")
    val subteamCreated       = Value("subteam_created")
    val subteamUpdated       = Value("subteam_updated")
    val subteamSelfAdded     = Value("subteam_self_added")
    val subteamSelfRemoved   = Value("subteam_self_removed")

    val reconnectUrl         = Value("reconnect_url")

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

  object MessageSubtypes extends Enumeration {

    type MessageSubtype = Value

    val unknown = Value

    val botMessage       = Value("bot_message")
    val meMessage        = Value("me_message")
    val messageChanged   = Value("message_changed")
    val messageDeleted   = Value("message_deleted")
    val channelJoin      = Value("channel_join")
    val channelLeave     = Value("channel_leave")
    val channelTopic     = Value("channel_topic")
    val channelPurpose   = Value("channel_purpose")
    val channelName      = Value("channel_name")
    val channelArchive   = Value("channel_archive")
    val channelUnarchive = Value("channel_unarchive")
    val groupJoin        = Value("group_join")
    val groupLeave       = Value("group_leave")
    val groupTopic       = Value("group_topic")
    val groupPurpose     = Value("group_purpose")
    val groupName        = Value("group_name")
    val groupArchive     = Value("group_archive")
    val groupUnarchive   = Value("group_unarchive")
    val fileShare        = Value("file_share")
    val fileComment      = Value("file_comment")
    val fileMention      = Value("file_mention")
    val pinnedItem       = Value("pinned_item")
    val unpinnedItem     = Value("unpinned_item")

    @transient
    implicit val writes = new Writes[MessageSubtype] {
      override def writes(o: MessageSubtype): JsValue = {
        JsString(o.toString)
      }
    }

    @transient
    implicit val reads = new Reads[MessageSubtype] {
      override def reads(json: JsValue): JsResult[MessageSubtype] = {
        JsSuccess {
          Try(MessageSubtypes.withName(
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

  case class SimpleInboundMessage(
    text: String,
    channel: Channel,
    user: User) extends TypedEvent(EventTypes.message)

  object SimpleInboundMessage {

    implicit val reads = {
      ((__ \ "text").read[String] ~
        (__ \ "channel").read[Channel] ~
        (__ \ "user").read[User]) (SimpleInboundMessage.apply _)
    }
  }

  import MessageSubtypes._

  case class ComplexInboundMessage(
    id: Option[Int],
    text: Option[String],
    channel: Option[Channel],
    user: Option[User],
    subType: Option[MessageSubtype]) extends TypedEvent(EventTypes.message)

  object ComplexInboundMessage {

    implicit val reads: Reads[ComplexInboundMessage] = {
      ((__ \ "id").readNullable[Int] ~
        (__ \ "text").readNullable[String] ~
        (__ \ "channel").readNullable[Channel] ~
        (__ \ "user").readNullable[User] ~
        (__ \ "subtype").readNullable[MessageSubtype]) (ComplexInboundMessage.apply _)
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
        (json \ "type").validate[EventType] match {
          case JsSuccess(eventType, _) => JsSuccess(TypedEvent(eventType))
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
            case JsSuccess(TypedEvent(EventTypes.message), _) =>

              json.validate[SimpleInboundMessage]
                .getOrElse(json.as[ComplexInboundMessage]
                  (ComplexInboundMessage.reads))

            case JsSuccess(in@TypedEvent(otherType), _) => in
            case JsError(_) => json.as[Posted]
          }
        }
      }
    }
  }
}
