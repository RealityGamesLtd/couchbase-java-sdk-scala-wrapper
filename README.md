# couchbase-java-sdk-scala-wrapper
Couchbase Java SDK wrapper for Scala with futures

## How to use
It is still work in progress. If reactive couchbase pains you so much you can clone the repo and build your own (`publishLocal`). Then the depenency is:
```"com.realitygames" %% "couchbase-java-sdk-scala-wrapper" % "0.1-SNAPSHOT"```.

## JSON Serialization


Currently project supports only `play-json`.

## Usage

Just import `com.realitygames.couchbase._` to use all necessary classes/methods/implicits.

```
import com.couchbase.client.java.CouchbaseCluster
import com.realitygames.couchbase._

val cluster = CouchbaseCluster.create("127.0.0.1")

val bucket = cluster.openBucket("test").scalaAsync()

case class User(
    name: String
)

object User {
    implicit val format: Format[User] = Json.format[User]
}
    
val user = User("Janusz")

//returns Future[User]
bucket.insert("someid", user)

//returns Future[User]
bucket.get[User]("someid")
```
