package com.realitygames.couchbase.model

case class Meta(
  id: String,
  rev: String,
  expiration: Int,
  flags: Int
)
