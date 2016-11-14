package com.realitygames.couchbase

import java.util.UUID
import io.circe.syntax._
import io.circe.generic.auto._
import com.couchbase.client.java.query.consistency.ScanConsistency
import com.couchbase.client.java.query.{N1qlParams, N1qlQuery}
import com.realitygames.couchbase.json.JsonFormatter
import com.realitygames.couchbase.models.User
import com.realitygames.couchbase.query.N1qlQueryResult
import com.realitygames.couchbase.query.N1qlQueryResult.SuccessN1qlQueryResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncWordSpec, _}
import com.realitygames.couchbase.json.circe._

class AsyncN1qlQueryTest extends AsyncWordSpec with MustMatchers with BucketTesting with ScalaFutures
   with RecoverMethods with Inside {

  override def bucketName: String = "users"

  def doTest[T: JsonFormatter](query: N1qlQuery)(check: N1qlQueryResult[T] => Assertion) =
    for {
      result <- bucket.query[T](query)
    } yield {
      check(result)
    }

  val alice = User("alice@mail.com", "Alice")
  val bob = User("bob@mail.com", "Bob")

  "AsyncBucket.query" should {

    val id = UUID.randomUUID().toString
    "insert user with n1sql" in {
      val query = s"""INSERT INTO `users` (KEY, VALUE) VALUES ("$id", ${bob.asJson.noSpaces}) RETURNING *"""
      doTest[User](N1qlQuery.simple(query)) { result =>
        result mustEqual SuccessN1qlQueryResult[User](Seq(bob), 1, Seq.empty)
      }
    }

    "select using an index" in {

      bucket.insert(UUID.randomUUID().toString, alice) flatMap { _ =>

        val query = "SELECT * FROM `users` USE INDEX (`user-index` USING VIEW)"
        val params = N1qlParams.build().consistency(ScanConsistency.REQUEST_PLUS)


        doTest[User](N1qlQuery.simple(query, params)) { result =>
          inside(result) {
            case SuccessN1qlQueryResult(values, _, _) =>
              values.map(_.email) must contain("alice@mail.com")
            case els: N1qlQueryResult[_] => fail
          }
        }
      }


    }
  }

}
