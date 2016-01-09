package com.cyberdolphins.slime

import play.api.libs.json.JsValue

/**
  * Created by mwielocha on 09/01/16.
  */
package object common {


  object Strings {

    implicit class HttpSafeString(underlying: String) {

      def escape = underlying.toString()
        .replaceAllLiterally("&", "&amp;")
        .replaceAllLiterally("<", "&lt;")
        .replaceAllLiterally(">", "&gt;")

    }
  }
}
