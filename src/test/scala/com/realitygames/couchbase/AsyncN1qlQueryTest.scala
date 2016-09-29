package com.realitygames.couchbase

import java.util.UUID

import com.couchbase.client.java.query.N1qlQuery
import com.realitygames.couchbase.models.User
import com.realitygames.couchbase.query.N1qlQueryResult
import com.realitygames.couchbase.query.N1qlQueryResult.SuccessN1qlQueryResult
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{AsyncWordSpec, _}
import play.api.libs.json.Format

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AsyncN1qlQueryTest extends AsyncWordSpec with MustMatchers with BucketTesting with ScalaFutures
   with RecoverMethods with Inside {

  override def bucketName: String = "users"


  def doTest[T: Format](query: N1qlQuery)(check: N1qlQueryResult[T] => Assertion) =
    for {
      result <- bucket.query[T](query)
    } yield {
      check(result)
    }

//  val alice = User("alice@mail.com", "Alice")
  val bob = User("bob@mail.com", "Bob")

  "AsyncBucket.query" should {

    val id = UUID.randomUUID().toString
    "insert user with n1sql" in {
      val query = s"""INSERT INTO `users` (KEY, VALUE) VALUES ("$id", ${User.format.writes(bob).toString()}) RETURNING *"""
      println(query)
      doTest[User](N1qlQuery.simple(query)) { result =>
        result mustEqual SuccessN1qlQueryResult[User](Seq(bob), 1, Seq.empty)
      }
    }

    "select using an index" in {

      doTest[User](N1qlQuery.simple("SELECT * FROM `users` USE INDEX (`user-index` USING VIEW)")) { result =>
        inside(result) {
          case SuccessN1qlQueryResult(values, _, _) =>
//            values.map(_.email) must contain("alice@mail.com")
            values.map(_.email) must contain("bob@mail.com")
          case els: N1qlQueryResult[_] => fail
        }
      }

    }
  }

}
