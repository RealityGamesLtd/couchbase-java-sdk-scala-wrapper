package com.realitygames.couchbase.util

import com.couchbase.client.java.document.{AbstractDocument, RawJsonDocument}
import com.realitygames.couchbase._
import com.realitygames.couchbase.model.Document
import com.realitygames.couchbase.json.{JsonReader, JsonWriter}
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}

protected[couchbase] object DocumentUtil extends LazyLogging {

  def createCouchbaseDocument[T](id: String, value: Option[T] = None, expiry: Int = 0, cas: CAS = 0l)(implicit jsonWrite: JsonWriter[T]): RawJsonDocument = {

    val json = value map jsonWrite.write getOrElse ""
    RawJsonDocument.create(
      id, expiry, json, cas
    )
  }

  def fromCouchbaseDocument[T](abstractDocument: AbstractDocument[_])(implicit jsonRead: JsonReader[T]): Document[T] = abstractDocument match {
    case document: RawJsonDocument => Document(
      id = document.id,
      cas = document.cas,
      content = jsonRead.read(document.content()) match {
        case Success(result) => result
        case Failure(errors) =>
          throw new Exception(errors)
      }
    )
  }
}
