package com.realitygames.couchbase

import com.couchbase.client.java.view.{Stale, ViewQuery}
import com.realitygames.couchbase.models.TestStructure
import com.realitygames.couchbase.query.QueryResult.SuccessQueryResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncWordSpec, _}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global

class ScalaAsyncBucketViewQueryTest extends AsyncWordSpec with MustMatchers with BucketTesting with ScalaFutures
  with RecoverMethods with Inside {
  val correctTestValueId = "example"
  val incorrectTestValueId = "example2"

  override def bucketName: String = "viewQuery"

  "AsyncBucket.query" should {
    "view get_object should return User value" in {
      for {
        result <- bucket.query[TestStructure](ViewQuery.from("testStructure", "get_object").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual TestStructure("string", 1, 2, 3, true, 4.5f, 5.5)
        }
      }
    }
    "view get_string should return string value" in {
      for {
        result <- bucket.query[String](ViewQuery.from("testStructure", "get_string").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual "string"
        }
      }
    }
    "view get_int should return int value" in {
      for {
        result <- bucket.query[Int](ViewQuery.from("testStructure", "get_int").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 1
        }
      }
    }
    "view get_long should return byte value" in {
      for {
        result <- bucket.query[Long](ViewQuery.from("testStructure", "get_long").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 2l
        }
      }
    }
    "view get_byte should return byte value" in {
      for {
        result <- bucket.query[Byte](ViewQuery.from("testStructure", "get_byte").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 3
        }
      }
    }
    "view get_boolean should return boolean value" in {
      for {
        result <- bucket.query[Boolean](ViewQuery.from("testStructure", "get_boolean").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual true
        }
      }
    }
    "view get_float should return float value" in {
      for {
        result <- bucket.query[Float](ViewQuery.from("testStructure", "get_float").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 4.5f
        }
      }
    }
    "view get_double should return double value" in {
      for {
        result <- bucket.query[Double](ViewQuery.from("testStructure", "get_double").key(correctTestValueId).limit(1).stale(Stale.FALSE))
      } yield {
        inside(result){
          case SuccessQueryResult(documents, _, _) =>
            documents.head.content mustEqual 5.5
        }
      }
    }
    "return all (2) documents for viewQuery bucket: 1 correct and 1 failed" in {
      for {
        result <- bucket.query[TestStructure](ViewQuery.from("testStructure", "get_object").stale(Stale.FALSE))
      } yield {

        inside(result){
          case SuccessQueryResult(documents, _, failedDocuments) =>
            documents.size mustBe 1
            failedDocuments.size mustBe 1

            documents.head.content mustEqual TestStructure("string", 1, 2, 3, true, 4.5f, 5.5)

            failedDocuments.head.errors must not be empty
            failedDocuments.head.raw mustEqual Json.parse(
              """{
                |  "string": 123,
                |  "int": 1,
                |  "long": 2,
                |  "byte": 3,
                |  "boolean": "true",
                |  "float": 4.5,
                |  "double": 5.5
                |}
              """.stripMargin)
        }
      }
    }
  }

}
