package com.realitygames.couchbase.model

import play.api.libs.json.{Format, Json}

case class Meta(
  id: String,
  rev: String,
  expiration: Int,
  flags: Int
)

object Meta {
  implicit val format: Format[Meta] = Json.format[Meta]
}
