package com.realitygames.couchbase

import com.couchbase.client.java.CouchbaseCluster
import org.scalatest.{AsyncWordSpecLike, BeforeAndAfterAll}

trait BucketTesting extends BeforeAndAfterAll { this: AsyncWordSpecLike =>

  def bucketName: String

  val bucket: AsyncBucket = {
    def cluster = CouchbaseCluster.create("127.0.0.1")
    cluster.openBucket(bucketName).scalaAsync()
  }

  override protected def afterAll(): Unit = {
    bucket.close()
  }

}
