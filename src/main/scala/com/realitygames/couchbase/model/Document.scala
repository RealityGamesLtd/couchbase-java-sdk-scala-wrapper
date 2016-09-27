package com.realitygames.couchbase.model

import com.realitygames.couchbase._

case class Document[T](
  id: String,
  cas: CAS,
  content: T
)
