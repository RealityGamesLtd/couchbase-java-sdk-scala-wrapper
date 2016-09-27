package com.realitygames.couchbase.query

import play.api.libs.json.JsValue

case class ParseFailedDocument(
  id: String,
  raw: JsValue,
  cause: Throwable
)
