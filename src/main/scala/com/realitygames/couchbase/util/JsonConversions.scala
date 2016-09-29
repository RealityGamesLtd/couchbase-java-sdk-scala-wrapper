package com.realitygames.couchbase.util

import java.util.{HashMap => JHashMap}

import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json._

import scala.collection.JavaConversions.{asScalaBuffer, mapAsScalaMap}
import scala.language.implicitConversions

protected[couchbase] trait JsonConversions {

   implicit class JsonObjectExt(underlying: JsonObject) {

     def toPlayJson: JsObject = couchbaseJsonObject2playJsObject(underlying)
  }


  def value2jsValue(value: Any): JsValue = {
    value match {
      case s: String =>
        JsString(s)
      case b: Boolean =>
        JsBoolean(b)
      case null =>
        JsNull
      case a: java.util.ArrayList[_] =>
        JsArray(a map value2jsValue)
      case i: Number =>
        JsNumber(BigDecimal(i.toString))
      case obj: JsonObject =>
        couchbaseJsonObject2playJsObject(obj)
      case obj: JHashMap[String, _] @unchecked=>
        JsObject(obj mapValues value2jsValue)
    }
  }

  implicit def couchbaseJsonObject2playJsObject(obj: JsonObject): JsObject =
    JsObject(obj.toMap mapValues value2jsValue)

}
