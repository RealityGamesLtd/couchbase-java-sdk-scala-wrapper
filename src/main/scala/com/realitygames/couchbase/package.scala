package com.realitygames

import com.couchbase.client.java.Bucket

package object couchbase {

  type CAS = Long

  implicit class Bucket2ScalaAsyncBucket(underlying: Bucket) {
    def scalaAsync(): AsyncBucket = {
      new AsyncBucket(underlying.async())
    }
  }
}
