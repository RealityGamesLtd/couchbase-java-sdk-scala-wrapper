package com.realitygames.couchbase.query

import com.couchbase.client.java.document.json.{JsonArray, JsonObject, JsonValue}
import com.couchbase.client.java.query.AsyncN1qlQueryRow
import com.couchbase.client.java.view.AsyncViewRow
import com.realitygames.couchbase.json.JsonReader
import com.realitygames.couchbase.model.Document

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

protected[couchbase] trait RowConversions {

  protected[couchbase] def asyncViewRow2document[T](
    view: AsyncViewRow
  )(
    implicit ec: ExecutionContext,
    jsonRead: JsonReader[T]
  ): Either[ParseFailedDocument, Document[T]] = {
    view.value() match {
      case json: JsonValue =>
        jsonRead.read(json.toString) match {
          case Failure(t) =>
            Left(ParseFailedDocument(view.id(), json, Seq(t)))
          case Success(content) =>
            Right(Document(
              id = view.id(),
              cas = 0l,
              content = content
            ))
        }
      case primitive: T @unchecked =>
        Try{
          Document[T](
            id = view.id(),
            cas = 0l,
            content = primitive
          )
        }.toEither.swap.map{t => ParseFailedDocument(view.id(), primitive, Seq(t))}.swap
    }
  }

  implicit protected[couchbase] def asyncN1qlRow2document[T](
    bucketName: String,
    view: AsyncN1qlQueryRow
  )(
    implicit ec: ExecutionContext,
    jsonRead: JsonReader[T]
  ): Option[Either[ParseFailedN1ql, T]] = {

    Try{
      val json = view.value().get(bucketName).toString
      jsonRead.read(json) match {
        case Failure(t) =>
          Left(ParseFailedN1ql(json, Seq(t)))
        case Success(content) =>
          Right(content)
      }
    }.toOption
  }

}
