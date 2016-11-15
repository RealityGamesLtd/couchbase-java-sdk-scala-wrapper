package com.realitygames.couchbase.json

import io.circe.parser._
import io.circe.{Decoder, Encoder, Error}

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object circe {

  private def toTry[L <: Throwable, R](either: Either[L, R]): Try[R] = {
    either.fold(left => Failure[R](left), right => Success[R](right))
  }

  implicit def encoder2writer[T](implicit encoder: Encoder[T]): JsonWriter[T] = {
    (obj: T) =>
      encoder(obj).toString()
  }

  implicit def decoder2reader[T](implicit decoder: Decoder[T]): JsonReader[T] = {
    (json: String) => {
      val result = parse(json).right.flatMap(decoder.decodeJson)
      toTry(result)
    }

  }

  implicit def writeAndRead2format[T](implicit encoder: Encoder[T], decoder: Decoder[T]): JsonFormatter[T] = new JsonFormatter[T] {

    override def read(json: String): Try[T] = {
      val result = parse(json).right.flatMap(decoder.decodeJson)
      toTry(result)
    }

    override def write(obj: T): String = encoder.apply(obj).toString()
  }

  implicit def func2Reader[T](f: String => Try[T]): JsonReader[T] = {
    new JsonReader[T] {
      override def read(json: String): Try[T] = f(json)
    }
  }

  implicit def func2Writer[T](f: T => String): JsonWriter[T] = {

    new JsonWriter[T] {
      override def write(obj: T): String = f(obj)
    }

  }

}
