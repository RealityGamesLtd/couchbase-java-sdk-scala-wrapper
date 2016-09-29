package com.realitygames.couchbase.query

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsValue}

case class ParseFailedDocument(
  id: String,
  raw: JsValue,
  errors: Seq[(JsPath, Seq[ValidationError])]
)

