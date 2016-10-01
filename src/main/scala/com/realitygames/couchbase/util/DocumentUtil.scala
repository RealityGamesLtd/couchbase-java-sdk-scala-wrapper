package com.realitygames.couchbase.util

import com.couchbase.client.java.document.{AbstractDocument, RawJsonDocument}
import com.realitygames.couchbase._
import com.realitygames.couchbase.model.Document
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._

protected[couchbase] object DocumentUtil extends LazyLogging {

  def createCouchbaseDocument[T](id: String, value: Option[T] = None, expiry: Int = 0, cas: CAS = 0l)(implicit writes: Writes[T]): RawJsonDocument = RawJsonDocument.create(
    id, expiry, Json.toJson(value).toString, cas
  )

  def fromCouchbaseDocument[T: Reads](abstractDocument: AbstractDocument[_]): Document[T] = abstractDocument match {
    case document: RawJsonDocument => Document(
      id = document.id,
      cas = document.cas,
      content = Json.parse(document.content()).validate[T] match {
        case JsSuccess(result, _) => result
        case JsError(errors) =>
          throw JsResultException(errors)
      }
    )
  }
}
