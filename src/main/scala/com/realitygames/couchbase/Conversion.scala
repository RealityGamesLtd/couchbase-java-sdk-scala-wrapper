package com.realitygames.couchbase

import java.util
import java.util.{HashMap => JHashMap}

import com.couchbase.client.java.document.RawJsonDocument
import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json._

import scala.collection.JavaConversions.{asScalaBuffer, mapAsScalaMap}
import scala.language.implicitConversions

object Conversion {

  protected[couchbase] implicit class JsonExt[T](underlying: T)(implicit write: Writes[T]) {

    def createCouchbaseDocument(id: String): RawJsonDocument = {
      RawJsonDocument.create(id, Json.toJson(underlying).toString)
    }

    def createCouchbaseDocument(id: String, cas: CAS): RawJsonDocument = {
      RawJsonDocument.create(id, Json.toJson(underlying).toString, cas)
    }
  }

  protected def value2jsValue(value: Any): JsValue = {
    value match {
      case s: String =>
        JsString(s)
      case b: Boolean =>
        JsBoolean(b)
      case null =>
        JsNull
      case a: util.ArrayList[_] =>
        JsArray(a map value2jsValue)
      case i: Number =>
        JsNumber(BigDecimal(i.toString))
      case obj: JsonObject =>
        douchbaseJsonObject2playJsObject(obj)
      case obj: JHashMap[String, _] @unchecked=>
        JsObject(obj mapValues value2jsValue)
    }
  }

  implicit def douchbaseJsonObject2playJsObject(obj: JsonObject): JsObject =
    JsObject(obj.toMap mapValues value2jsValue)

}
