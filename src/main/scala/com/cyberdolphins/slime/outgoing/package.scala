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

  case class SimpleMessage(
    id: Option[Int],
    text: String,
    user: Option[String],
    channel: Option[String]) extends Outbound {

    override def stamp(id: Int): Outbound = copy(id = Some(id))
  }

  object SimpleMessage {

    def apply(text: String): SimpleMessage = {
      new SimpleMessage(None, text, None, None)
    }

    def apply(text: String, channel: String): SimpleMessage = {
      new SimpleMessage(None, text, None, channel = Some(channel))
    }

    def apply(text: String, channel: String, user: String): SimpleMessage = {
      new SimpleMessage(None, text, user = Some(user), channel = Some(channel))
    }

    private val _writes: Writes[SimpleMessage] = {
      ((__ \ "id").writeNullable[Int] ~
        (__ \ "text").write[String] ~
        (__ \ "user").writeNullable[String] ~
        (__ \ "channel").writeNullable[String]) (unlift(SimpleMessage.unapply))
    }

    implicit val writes = new Writes[SimpleMessage] {
      override def writes(o: SimpleMessage): JsValue = {
        Json.toJson(o)(_writes).as[JsObject] ++ Json.obj("type" -> message)
      }
    }
  }

  case class Message(
    id: Option[Int],
    text: String,
    channel: String,
    user: Option[String],
    attachments: Option[Seq[Attachment]]) extends Outbound {

    override def stamp(id: Int): Outbound = copy(id = Some(id))
  }

  object Message {

    def apply(text: String, channel: String): Message = {
      new Message(None, text, channel, None, None)
    }

    def apply(text: String, channel: String, user: String): Message = {
      new Message(None, text, channel, Some(user), None)
    }

    def apply(text: String, channel: String, user: String, attachments: Attachment*): Message = {
      new Message(None, text, channel, Some(user), Some(attachments))
    }

    private val _writes: Writes[Message] = {
      ((__ \ "id").writeNullable[Int] ~
        (__ \ "text").write[String] ~
        (__ \ "channel").write[String] ~
        (__ \ "user").writeNullable[String] ~
        (__ \ "user").writeNullable[Seq[Attachment]]) (unlift(Message.unapply))
    }

    implicit val writes = new Writes[Message] {
      override def writes(o: Message): JsValue = {
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
          case o: SimpleMessage => Json.toJson(o)
          case o: Message => Json.toJson(o)
          case o: Ping => Json.toJson(o)
        }
      }
    }
  }
}
