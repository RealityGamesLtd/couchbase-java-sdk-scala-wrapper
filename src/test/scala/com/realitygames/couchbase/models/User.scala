package com.realitygames.couchbase.models

import play.api.libs.json.{Format, Json}

case class User(
  email: String,
  name: String
)

object User {
  implicit val format: Format[User] = Json.format[User]
}
