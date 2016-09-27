//package com.realitygames.couchbase
//
//import com.couchbase.client.java.query.N1qlQuery
//import com.realitygames.couchbase.models.TestStructure
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.{AsyncWordSpec, _}
//
//import scala.concurrent.ExecutionContext.Implicits.global
//
//class AsyncN1qlQueryTest extends AsyncWordSpec with MustMatchers with BucketTesting with ScalaFutures
//   with RecoverMethods with Inside {
//
//  override def bucketName: String = "viewQuery"
//
//
//  def doTest[T](query: N1qlQuery)(expected: QueryResult[T]) =
//    for {
//      result <- bucket.query[TestStructure](query)
//    } yield {
//      expected mustBe expected
//    }
//
//
//  "AsyncBucket.query" should {
//    "view get_object should return User value" in {
//
//      doTest(N1qlQuery.simple(select("*").from(i("testStructure")).limit(10)))
//
//    }
//  }
//
//}
