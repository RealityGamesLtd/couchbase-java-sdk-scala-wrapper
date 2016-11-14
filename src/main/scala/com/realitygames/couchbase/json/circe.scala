package com.realitygames.couchbase.json

import io.circe.parser._
import io.circe.{Decoder, Encoder}

import scala.util.Try

object circe {

  implicit def encoder2writer[T](implicit encoder: Encoder[T]): JsonWriter[T] = (obj: T) =>
    encoder.apply(obj).toString()
  implicit def decoder2reader[T](implicit decoder: Decoder[T]): JsonReader[T] = (json: String) => {
    parse(json).toTry flatMap { parsed =>
      decoder.decodeJson(parsed).toTry
    }
  }

  implicit def writeAndRead2format[T](implicit reader: JsonReader[T], writer: JsonWriter[T]): JsonFormatter[T] = new JsonFormatter[T] {

    override def read(json: String): Try[T] = reader.read(json)

    override def write(obj: T): String = writer.write(obj)
  }
}
