package com.realitygames.couchbase.json

import scala.util.Try

trait JsonReader[T] {

  def read(json: String): Try[T]
}
