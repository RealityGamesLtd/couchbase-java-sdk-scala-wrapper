package com.realitygames.couchbase

import com.couchbase.client.java.document.{JsonDocument, RawJsonDocument}
import com.realitygames.couchbase.Conversion._
import play.api.libs.json.{Json, Reads}

case class Document[T](
  id: String,
  expiry: Expiration,
  cas: CAS,
  content: T
)

object Document {
  private[couchbase] def fromCouchbaseDoc[T](doc: JsonDocument)(implicit reads: Reads[T]): Document[T] = {
    Document(
      doc.id,
      Expiration.fromSeconds(doc.expiry),
      doc.cas,
      doc.content.validate[T].get
    )
  }
  private[couchbase] def fromRawCouchbaseDoc[T](doc: RawJsonDocument)(implicit reads: Reads[T]): Document[T] = {
    Document(
      doc.id,
      Expiration.fromSeconds(doc.expiry),
      doc.cas,
      Json.parse(doc.content).validate[T].get
    )
  }
}
