package com.realitygames.couchbase.query

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsValue}

case class ParseFailedN1ql(
  raw: JsValue,
  errors: Seq[(JsPath, Seq[ValidationError])]
)
