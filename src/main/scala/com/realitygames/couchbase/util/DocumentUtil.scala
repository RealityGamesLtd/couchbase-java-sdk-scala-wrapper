package com.realitygames.couchbase.util

import com.couchbase.client.java.document.{AbstractDocument, RawJsonDocument}
import com.realitygames.couchbase._
import com.realitygames.couchbase.model.Document
import play.api.libs.json.{Json, Reads, Writes}

protected[couchbase] object DocumentUtil {

  def createCouchbaseDocument[T](id: String, value: Option[T] = None, expiry: Int = 0, cas: CAS = 0l)(implicit writes: Writes[T]): RawJsonDocument = RawJsonDocument.create(
    id, expiry, Json.toJson(value).toString, cas
  )

  def fromCouchbaseDocument[T](abstractDocument: AbstractDocument[_])(implicit reads: Reads[T]): Document[T] = abstractDocument match {
    case document: RawJsonDocument => Document(
      id = document.id,
      cas = document.cas,
      content = Json.parse(document.content()).validate[T].get
    )
  }
}
