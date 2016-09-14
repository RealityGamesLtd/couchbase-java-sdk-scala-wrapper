package com.realitygames.couchbase

import java.util.UUID

import com.couchbase.client.java.error.{DocumentAlreadyExistsException, DocumentDoesNotExistException}
import com.github.nscala_time.time.Imports._
import com.realitygames.couchbase.models.User
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AsyncBucketBasicOperationsTest extends AsyncWordSpec with MustMatchers with TestBucket with ScalaFutures with BeforeAndAfterAll with RecoverMethods {

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
      bucket.exists("exists_id_test_1") map {exists =>
        exists mustBe false
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
  }
}
