package com.realitygames.couchbase.json

trait JsonWriter[T] {

  def write(obj: T): String
}
