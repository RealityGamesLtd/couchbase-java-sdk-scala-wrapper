package com.realitygames.couchbase.model

import org.joda.time.DateTime

case class Expiration(value: Option[DateTime]) extends AnyVal {
  def seconds: Int = value map {v => (v.getMillis/1000).toInt} getOrElse 0
}

object Expiration {

  val none: Expiration = Expiration(None)

  def fromSeconds(seconds: Int): Expiration = {
    seconds match {
      case 0 =>
        Expiration(None)
      case i =>
        Expiration(Some(new DateTime(seconds*1000L)))
    }
  }

  def apply(value: DateTime): Expiration = Expiration(Some(value))
}
