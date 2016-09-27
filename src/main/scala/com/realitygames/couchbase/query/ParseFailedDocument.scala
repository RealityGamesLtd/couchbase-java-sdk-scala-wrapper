package com.realitygames.couchbase.query

import play.api.libs.json.JsValue

case class ParseFailedDocument(
  raw: JsValue,
  error: Throwable
)
