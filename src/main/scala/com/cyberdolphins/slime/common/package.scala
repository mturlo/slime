package com.cyberdolphins.slime

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.Try

/**
  * Created by mwielocha on 09/01/16.
  */
package object common {

  object Strings {

    implicit class HttpSafeString(underlying: String) {

      def escape = {
        underlying.toString()
          .replaceAllLiterally("&", "&amp;")
          .replaceAllLiterally("<", "&lt;")
          .replaceAllLiterally(">", "&gt;")
      }
    }
  }

  case class Channel(name: String) extends AnyVal

  object Channel {
    implicit val reads = new Reads[Channel] {
      override def reads(json: JsValue): JsResult[Channel] = {
        JsSuccess(Channel(json.as[String]))
      }
    }

    implicit val writes = new Writes[Channel] {
      override def writes(o: Channel): JsValue = {
        JsString(o.name)
      }
    }
  }

  case class User(name: String) extends AnyVal

  object User {
    implicit val reads = new Reads[User] {
      override def reads(json: JsValue): JsResult[User] = {
        JsSuccess(User(json.as[String]))
     }
    }

    implicit val writes = new Writes[User] {
      override def writes(o: User): JsValue = {
        JsString(o.name)
      }
    }
  }

  case class Color(value: String) extends AnyVal

  object Color {

    val good    = Color("good")
    val warning = Color("warning")
    val danger  = Color("danger")

    implicit val reads = new Reads[Color] {
      override def reads(json: JsValue): JsResult[Color] = {
        JsSuccess(Color(json.as[String]))
      }
    }

    implicit val writes = new Writes[Color] {
      override def writes(o: Color): JsValue = {
        JsString(o.value)
      }
    }
  }

  object MarkdownInValues extends Enumeration {

    type MarkdownInValue = Value

    private val unknown = Value

    val preText = Value("pretext")
    val text    = Value("text")
    val fields  = Value("fields")

    @transient
    implicit val reads = new Reads[MarkdownInValue] {
      override def reads(json: JsValue): JsResult[MarkdownInValue] = {
        JsSuccess(Try(withName(json.as[String])).getOrElse(unknown))
      }
    }

    @transient
    implicit val writes = new Writes[MarkdownInValue] {
      override def writes(o: MarkdownInValue): JsValue = {
        JsString(o.toString)
      }
    }

  }

  case class Field(
    value: Option[String],
    title: Option[String],
    short: Option[Boolean])

  object Field {

    def apply(value: String) = {
      new Field(Some(value), None, None)
    }

    def apply(value: String, title: String) = {
      new Field(Some(value), Some(title), None)
    }

    def apply(value: String, title: String, short: Boolean) = {
      new Field(Some(value), Some(title), short = Some(short))
    }

    def _apply(value: Option[String], title: Option[String], short: Option[Boolean]) = {
      new Field(value, title, short = short)
    }

    implicit val writes = {
      ((__ \ "value").writeNullable[String] ~
        (__ \ "title").writeNullable[String] ~
        (__ \ "short").writeNullable[Boolean]) (unlift(Field.unapply))
    }

    implicit val reads = {
      ((__ \ "value").readNullable[String] ~
        (__ \ "title").readNullable[String] ~
        (__ \ "short").readNullable[Boolean]) (Field._apply _)
    }
  }

  import MarkdownInValues._

  case class Attachment(
     text: Option[String],
     title: Option[String],
     titleLink: Option[String],
     fields: Option[List[Field]],
     markdown: Option[Boolean],
     color: Option[Color],
     markdownIn: Option[List[MarkdownInValue]],
     imageUrl: Option[String]) {

    def withFields(fields: Field*) = {
      copy(fields = Some(fields.toList))
    }

    def withColor(color: Color) = {
      copy(color = Some(color))
    }

    def withMarkdownIn(values: MarkdownInValue*) = {
      copy(markdownIn = Some(values.toList))
    }

    def withImageUrl(imageUrl: String): Attachment = {
      copy(imageUrl = Some(imageUrl))
    }
  }

  object Attachment {

    def apply() = {
      new Attachment(None, None, None, None, None, None, None, None)
    }

    def apply(fields: Field*) = {
      new Attachment(None, None, None, Some(fields.toList), None, None, None, None)
    }

    def apply(text: String) = {
      new Attachment(Some(text), None, None, None, None, None, None, None)
    }

    def apply(title: String, text: String) = {
      new Attachment(Some(text), Some(title), None, None, None, None, None, None)
    }

    def apply(title: String, text: String, fields: Field*) = {
      new Attachment(Some(text), Some(title), None, Some(fields.toList), None, None, None, None)
    }

    def apply(title: String, titleLink: String, text: String) = {
      new Attachment(Some(text), Some(title), Some(titleLink), None, None, None, None, None)
    }

    def apply(title: String, titleLink: String, text: String, markdown: Boolean) = {
      new Attachment(Some(text), Some(title),
        Some(titleLink), None, Some(markdown), None, None, None)
    }

    def apply(title: String, titleLink: String, text: String, markdown: Boolean, color: Color) = {
      new Attachment(Some(text), Some(title),
        Some(titleLink), None, Some(markdown), Some(color), None, None)
    }

    def apply(title: String, titleLink: String, text: String, markdown: Boolean, color: Color, markdownIn: List[MarkdownInValue]) = {
      new Attachment(Some(text), Some(title),
        Some(titleLink), None, Some(markdown), Some(color), Some(markdownIn), None)
    }

    implicit val writes = {
      ((__ \ "text").writeNullable[String] ~
        (__ \ "title").writeNullable[String] ~
        (__ \ "title_link").writeNullable[String] ~
        (__ \ "fields").writeNullable[List[Field]] ~
        (__ \ "mrkdwn").writeNullable[Boolean] ~
        (__ \ "color").writeNullable[Color] ~
        (__ \ "mrkdwn_in").writeNullable[List[MarkdownInValue]] ~
        (__ \ "image_url").writeNullable[String]) (unlift(Attachment.unapply))
    }
  }
}
