package com.cyberdolphins.slime

import play.api.libs.json._
import play.api.libs.functional.syntax._

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
    user: Option[String],
    channel: Option[String]) extends Outbound {

    override def stamp(id: Int): Outbound = copy(id = Some(id))
  }

  object SimpleOutboundMessage {

    def apply(text: String): SimpleOutboundMessage = {
      new SimpleOutboundMessage(None, text, None, None)
    }

    def apply(text: String, channel: String): SimpleOutboundMessage = {
      new SimpleOutboundMessage(None, text, None, channel = Some(channel))
    }

    def apply(text: String, channel: String, user: String): SimpleOutboundMessage = {
      new SimpleOutboundMessage(None, text, user = Some(user), channel = Some(channel))
    }

    private val _writes: Writes[SimpleOutboundMessage] = {
      ((__ \ "id").writeNullable[Int] ~
        (__ \ "text").write[String] ~
        (__ \ "user").writeNullable[String] ~
        (__ \ "channel").writeNullable[String]) (unlift(SimpleOutboundMessage.unapply))
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
    channel: String,
    user: Option[String],
    attachments: Option[Seq[Attachment]]) extends Outbound {

    override def stamp(id: Int): Outbound = copy(id = Some(id))
  }

  object ComplexOutboundMessage {

    def apply(text: String, channel: String): ComplexOutboundMessage = {
      new ComplexOutboundMessage(None, text, channel, None, None)
    }

    def apply(text: String, channel: String, user: String): ComplexOutboundMessage = {
      new ComplexOutboundMessage(None, text, channel, Some(user), None)
    }

    def apply(text: String, channel: String, user: String, attachments: Attachment*): ComplexOutboundMessage = {
      new ComplexOutboundMessage(None, text, channel, Some(user), Some(attachments))
    }

    private val _writes: Writes[ComplexOutboundMessage] = {
      ((__ \ "id").writeNullable[Int] ~
        (__ \ "text").write[String] ~
        (__ \ "channel").write[String] ~
        (__ \ "user").writeNullable[String] ~
        (__ \ "user").writeNullable[Seq[Attachment]]) (unlift(ComplexOutboundMessage.unapply))
    }

    implicit val writes = new Writes[ComplexOutboundMessage] {
      override def writes(o: ComplexOutboundMessage): JsValue = {
        Json.toJson(o)(_writes).as[JsObject] ++ Json.obj("type" -> message)
      }
    }
  }

  case class Attachment(text: Option[String], title: Option[String], titleLink: Option[String])

  object Attachment {

    def apply(text: String) = {
      new Attachment(Some(text), None, None)
    }

    def apply(title: String, text: String) = {
      new Attachment(Some(text), Some(title), None)
    }

    def apply(title: String, titleLink: String, text: String) = {
      new Attachment(Some(text), Some(title), Some(titleLink))
    }

    implicit val writes = {
      ((__ \ "text").writeNullable[String] ~
        (__ \ "title").writeNullable[String] ~
        (__ \ "title_link").writeNullable[String]) (unlift(Attachment.unapply))
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
