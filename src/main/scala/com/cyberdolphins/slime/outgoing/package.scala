package com.cyberdolphins.slime

import com.cyberdolphins.slime.common._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Created by mwielocha on 09/01/16.
  */
package object outgoing {

  object MessageTypes extends Enumeration {

    type MessageType = Value

    val message = Value
    val ping    = Value

    @transient
    implicit val writes = new Writes[MessageType] {
      override def writes(o: MessageType): JsValue = {
        JsString(o.toString)
      }
    }
  }

  import MessageTypes._

  sealed trait Outbound {
    def id: Option[Int]
    def stamp(id: Int): Outbound
  }

  case class Ping(id: Option[Int]) extends Outbound {
    override def stamp(id: Int): Outbound = copy(id = Some(id))
  }

  object Ping {

    private val _writes = Json.writes[Ping]

    implicit val writes = new Writes[Ping] {
      override def writes(o: Ping): JsValue = {
        Json.toJson(o)(_writes).as[JsObject] ++ Json.obj("type" -> ping)
      }
    }
  }

  case class SimpleOutboundMessage(
    id: Option[Int],
    text: String,
    channel: Option[Channel],
    user: Option[User]) extends Outbound {

    override def stamp(id: Int): Outbound = copy(id = Some(id))
  }

  object SimpleOutboundMessage {

    def apply(text: String): SimpleOutboundMessage = {
      new SimpleOutboundMessage(None, text, None, None)
    }

    def apply(text: String, channel: Channel): SimpleOutboundMessage = {
      new SimpleOutboundMessage(id = None, text = text, user = None, channel = Some(channel))
    }

    def apply(text: String, channel: Channel, user: User): SimpleOutboundMessage = {
      new SimpleOutboundMessage(None, text, user = Some(user), channel = Some(channel))
    }

    private val _writes: Writes[SimpleOutboundMessage] = {
      ((__ \ "id").writeNullable[Int] ~
        (__ \ "text").write[String] ~
        (__ \ "channel").writeNullable[Channel] ~
        (__ \ "user").writeNullable[User]) (unlift(SimpleOutboundMessage.unapply))
    }

    implicit val writes = new Writes[SimpleOutboundMessage] {
      override def writes(o: SimpleOutboundMessage): JsValue = {
        Json.toJson(o)(_writes).as[JsObject] ++ Json.obj("type" -> message)
      }
    }
  }

  case class ComplexOutboundMessage(
    id: Option[Int],
    text: String,
    channel: Channel,
    user: Option[User],
    attachments: Option[Seq[Attachment]]) extends Outbound {

    override def stamp(id: Int): Outbound = copy(id = Some(id))

    def withAttachments(attachments: Attachment*): ComplexOutboundMessage = {
      copy(attachments = Some(attachments))
    }
  }

  object ComplexOutboundMessage {

    def apply(text: String, channel: Channel): ComplexOutboundMessage = {
      new ComplexOutboundMessage(None, text, channel, None, None)
    }

    def apply(text: String, channel: Channel, attachments: Attachment*): ComplexOutboundMessage = {
      new ComplexOutboundMessage(None, text, channel, None, Some(attachments))
    }

    def apply(text: String, channel: Channel, user: User) = {
      new ComplexOutboundMessage(None, text, channel, Some(user), None)
    }

    def apply(text: String, channel: Channel, user: User, attachments: Attachment*): ComplexOutboundMessage = {
      new ComplexOutboundMessage(None, text, channel, Some(user), Some(attachments))
    }

    private val _writes: Writes[ComplexOutboundMessage] = {
      ((__ \ "id").writeNullable[Int] ~
        (__ \ "text").write[String] ~
        (__ \ "channel").write[Channel] ~
        (__ \ "user").writeNullable[User] ~
        (__ \ "attachments").writeNullable[Seq[Attachment]]) (unlift(ComplexOutboundMessage.unapply))
    }

    implicit val writes = new Writes[ComplexOutboundMessage] {
      override def writes(o: ComplexOutboundMessage): JsValue = {
        Json.toJson(o)(_writes).as[JsObject] ++ Json.obj("type" -> message)
      }
    }
  }

  object Outbound {

    implicit val writes = new Writes[Outbound] {
      override def writes(o: Outbound): JsValue = {
        o match {
          case o: SimpleOutboundMessage => Json.toJson(o)
          case o: ComplexOutboundMessage => Json.toJson(o)
          case o: Ping => Json.toJson(o)
        }
      }
    }
  }
}
