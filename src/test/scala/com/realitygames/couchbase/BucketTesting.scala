package com.realitygames.couchbase

import com.couchbase.client.java.CouchbaseCluster

trait BucketTesting {

  def bucketName: String

  val bucket: AsyncBucket = {
    def cluster = CouchbaseCluster.create("127.0.0.1")
    cluster.openBucket(bucketName).scalaAsync()
  }
}
