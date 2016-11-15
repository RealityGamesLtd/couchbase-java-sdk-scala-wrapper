package com.realitygames.couchbase

import java.util.UUID

import com.couchbase.client.java.error.{DocumentAlreadyExistsException, DocumentDoesNotExistException}
import com.couchbase.client.java.view.{Stale, ViewQuery}
import com.github.nscala_time.time.Imports._
import com.realitygames.couchbase.json.circe._
import com.realitygames.couchbase.model.{Expiration, Meta}
import com.realitygames.couchbase.models.User
import com.realitygames.couchbase.query.QueryResult.SuccessQueryResult
import io.circe.generic.auto._
import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class ScalaAsyncBucketBasicOperationsTest extends AsyncWordSpec with MustMatchers with BucketTesting with ScalaFutures with BeforeAndAfterAll with RecoverMethods with Inside {

  override def bucketName: String = "users"

  override protected def afterAll(): Unit = {
    bucket.close()
  }

  def getId: String = UUID.randomUUID().toString

  "AsyncBucket.atomicUpdate" should {
    "do atomic update if document exists" in {

      val id = getId
      val user = User("a@b.c", "noname")
      val newUser = User("sample@email.com", "newname")

      bucket.insert(id, user) flatMap  { doc =>
        bucket.atomicUpdate[User](id){doc =>
          Future.successful(doc.content.copy(email = "new_email@aaa.bb"))
        } map { doc =>
          doc.content.email mustEqual "new_email@aaa.bb"
        }
      }
    }
  }

  "AsyncBucket.exists" should {
    "return false if document doesn't exist" in {
      bucket.exists("exists_id_test_1") map { exists =>
        exists mustBe false
      }
    }
  }

  "AsyncBucket.insert" should {
    "insert if document with id doesn't exist" in {
      val id = getId
      val user = User("a@b.c", "noname")
      bucket.insert(id, user) map { doc =>
        doc.content mustEqual user
        doc.id mustEqual id
      }
    }

    "fail to overwrite document" in {
      val id = getId
      val user = User("a@b.c", "noname")

      bucket.insert(id, user) flatMap { _ =>
        recoverToSucceededIf[DocumentAlreadyExistsException] {
          bucket.insert(id, user)
        }
      }
    }

    "insert String" in {
      val id = getId
      bucket.insert(id, "string") map { doc =>
        doc.id mustEqual id
      }
    }
  }

  "AsyncBucket.remove" should {
    "fail to remove non-existing document" in {
      val id = getId

      recoverToSucceededIf[DocumentDoesNotExistException] {
        bucket.remove(id)
      }
    }

    "remove existing document" in {
      val id = getId
      val user = User("a@b.c", "noname")

      for {
        _ <- bucket.insert(id, user)
        _ <- bucket.remove(id)
        e <- bucket.exists(id)
      } yield {
        e mustEqual false
      }
    }
  }

  "AsyncBucket.get" should {
    "return existing document" in {
      val id = getId
      val user = User("a@b.c", "noname")

      for {
        _ <- bucket.insert(id, user)
        document <- bucket.get[User](id)
      } yield {
        document.content mustEqual user
      }
    }

    "fail to get non-existing document" in {
      val id = getId
      val user = User("a@b.c", "noname")

      recoverToSucceededIf[NoSuchElementException]{
        bucket.get[User](id)
      }
    }

    "retrieve string-value document" in {
      val id = getId

      for {
        _ <- bucket.insert(id, "somestring")
        doc <- bucket.get[String](id)
      } yield {
        doc.content mustEqual "somestring"
      }
    }

    "retrieve int-value document" in {
      val id = getId

      for {
        _ <- bucket.insert(id, 12345)
        doc <- bucket.get[Int](id)
      } yield {
        doc.content mustEqual 12345
      }
    }

    "retrieve boolean-value document" in {
      val id = getId

      for {
        _ <- bucket.insert(id, true)
        doc <- bucket.get[Boolean](id)
      } yield {
        doc.content mustEqual true
      }
    }
  }

  "AsyncBucket.replace" should {
    "do replace existing document" in {
      val id = getId
      val user = User("a@b.c", "noname")
      val newUser = User("sample@email.com", "newname")

      for {
        _ <- bucket.insert(id, user)
        _ <- bucket.replace(id, newUser)
        document <- bucket.get[User](id)
      } yield {
        document.content mustEqual newUser
      }
    }

    "fail to replace non-existing document" in {
      val id = getId
      val user = User("a@b.c", "noname")
      val newUser = User("sample@email.com", "newname")

      recoverToSucceededIf[DocumentDoesNotExistException]{
        bucket.replace(id, newUser)
      }
    }
  }

  "AsyncBucket.upset" should {
    "do upsert existing document" in {
      val id = getId
      val user = User("a@b.c", "noname")
      val newUser = User("sample@email.com", "newname")

      for {
        _ <- bucket.insert(id, user)
        _ <- bucket.upsert(id, newUser)
        document <- bucket.get[User](id)
      } yield {
        document.content mustEqual newUser
      }
    }

    "upsert new document" in {
      val id = getId
      val user = User("a@b.c", "noname")

      for {
        _ <- bucket.upsert(id, user)
        document <- bucket.get[User](id)
      } yield {
        document.content mustEqual user
      }
    }
  }

  "AsyncBucket.getAndTouch" should {
    "fail to get non-existing document" in {
      val id = getId
      val expiry = Expiration(DateTime.now())

      recoverToSucceededIf[NoSuchElementException ]{
        bucket.getAndTouch[User](id, expiry)
      }
    }
  }

  "insert document with no expiration time" in {
    val id = getId
    val user = User("a@b.c", "noname")

    for {
      _ <- bucket.insert(id, user)
      meta <- bucket.query[Meta](ViewQuery.from("user", "meta").key(id).stale(Stale.FALSE))
    } yield {
      inside(meta){
        case SuccessQueryResult(Seq(userMeta), _, _) =>
          userMeta.content.id mustEqual id
          userMeta.content.expiration mustEqual 0
      }
    }
  }

  "insert document with short expiration (<30d)" in {
    val id = getId
    val user = User("a@b.c", "noname")

    val expiration = Expiration(DateTime.now() + 10.days)

    for {
      _ <- bucket.insert(id, user, expiration)
      meta <- bucket.query[Meta](ViewQuery.from("user", "meta").key(id).stale(Stale.FALSE))
    } yield {
      inside(meta){
        case SuccessQueryResult(Seq(userMeta), _, _) =>
          userMeta.content.id mustEqual id
          userMeta.content.expiration mustEqual expiration.seconds
      }
    }
  }

  "insert document with short expiration (>30d)" in {
    val id = getId
    val user = User("a@b.c", "noname")

    val expiration = Expiration(DateTime.now() + 150.days)

    for {
      _ <- bucket.insert(id, user, expiration)
      meta <- bucket.query[Meta](ViewQuery.from("user", "meta").key(id).stale(Stale.FALSE))
    } yield {
      inside(meta){
        case SuccessQueryResult(Seq(userMeta), _, _) =>
          userMeta.content.id mustEqual id
          userMeta.content.expiration mustEqual expiration.seconds
      }
    }
  }

}
