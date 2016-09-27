package com.realitygames

import com.couchbase.client.java.Bucket

package object couchbase {

  @deprecated("use ScalaAsyncBucket")
  type AsyncBucket = ScalaAsyncBucket

  type CAS = Long

  implicit class Bucket2ScalaAsyncBucket(underlying: Bucket) {
    def scalaAsync(): ScalaAsyncBucket = {
      new ScalaAsyncBucket(underlying.async())
    }
  }
}
