package com.realitygames.couchbase.query

case class ParseFailedN1ql(
  raw: Any,
  errors: Seq[Any]
)
