package com.realitygames.couchbase

import com.typesafe.config.Config

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.util.Try

case class BucketConfiguration(
  hosts: List[String],
  bucket: String,
  password: Option[String],
  timeout: Option[Duration]
)

object BucketConfiguration {
  def fromConfig(config: Config): BucketConfiguration = {
    BucketConfiguration(
      config.getStringList("hosts").toList,
      config.getString("bucket"),
      Try(config.getString("password")).toOption,
      Try(Duration(config.getString("timeout"))).toOption
    )
  }
}
