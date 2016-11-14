package com.realitygames.couchbase.query

case class ParseFailedDocument(
  id: String,
  raw: Any,
  errors: Seq[Any] //TODO: should we have some structure for errors or just pass errors from json libraries?
)

