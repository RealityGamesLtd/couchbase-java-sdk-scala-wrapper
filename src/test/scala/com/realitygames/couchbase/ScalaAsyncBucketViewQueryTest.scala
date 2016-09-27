package com.realitygames.couchbase

import com.couchbase.client.java.view.{Stale, ViewQuery}
import com.realitygames.couchbase.query.QueryResult.SuccessQueryResult
import com.realitygames.couchbase.models.TestStructure
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncWordSpec, _}

import scala.concurrent.ExecutionContext.Implicits.global

class ScalaAsyncBucketViewQueryTest extends AsyncWordSpec with MustMatchers with BucketTesting with ScalaFutures
  with RecoverMethods with Inside {
  val testValueId = "example"

  override def bucketName: String = "viewQuery"

  "AsyncBucket.query" should {
    "view get_object should return User value" in {
      for {
        result <- bucket.query[TestStructure](ViewQuery.from("testStructure", "get_object").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual TestStructure("string", 1, 2, 3, true, 4.5f, 5.5)
        }
      }
    }
    "view get_string should return string value" in {
      for {
        result <- bucket.query[String](ViewQuery.from("testStructure", "get_string").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual "string"
        }
      }
    }
    "view get_int should return int value" in {
      for {
        result <- bucket.query[Int](ViewQuery.from("testStructure", "get_int").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 1
        }
      }
    }
    "view get_long should return byte value" in {
      for {
        result <- bucket.query[Long](ViewQuery.from("testStructure", "get_long").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 2l
        }
      }
    }
    "view get_byte should return byte value" in {
      for {
        result <- bucket.query[Byte](ViewQuery.from("testStructure", "get_byte").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 3
        }
      }
    }
    "view get_boolean should return boolean value" in {
      for {
        result <- bucket.query[Boolean](ViewQuery.from("testStructure", "get_boolean").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual true
        }
      }
    }
    "view get_float should return float value" in {
      for {
        result <- bucket.query[Float](ViewQuery.from("testStructure", "get_float").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 4.5f
        }
      }
    }
    "view get_double should return double value" in {
      for {
        result <- bucket.query[Double](ViewQuery.from("testStructure", "get_double").key(testValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 5.5
        }
      }
    }
  }

}
