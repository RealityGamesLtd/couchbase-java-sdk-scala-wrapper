package com.realitygames.couchbase.model

import com.couchbase.client.java.document.JsonDocument
import com.realitygames.couchbase._

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
