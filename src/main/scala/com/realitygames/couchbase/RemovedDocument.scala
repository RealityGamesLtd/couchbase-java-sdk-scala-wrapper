package com.realitygames.couchbase

import com.couchbase.client.java.document.JsonDocument
import com.realitygames.couchbase.model.Expiration

case class RemovedDocument(
  expiry: Expiration,
  cas: CAS
)

object RemovedDocument {
  private[couchbase] def fromCouchbaseDoc(doc: JsonDocument): RemovedDocument = {
    RemovedDocument(
      Expiration.fromSeconds(doc.expiry()),
      doc.cas()
    )
  }
}
