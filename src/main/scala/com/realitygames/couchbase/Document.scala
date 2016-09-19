package com.realitygames.couchbase

case class Document[T](
  id: String,
  cas: CAS,
  content: T
)
