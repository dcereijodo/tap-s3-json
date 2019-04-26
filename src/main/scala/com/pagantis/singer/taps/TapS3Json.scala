package com.pagantis.singer.taps

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContextExecutor

object TapS3Json extends App {

  val clazz = getClass.getName

  // parse config
  val config = ConfigFactory.load().getConfig("tap")
  import net.ceedubs.ficus.Ficus._
  val bucketName = config.getString("bucket_name")
  val s3Preffix = config.as[Option[String]]("s3_preffix")
  val jsonPaths = config.as[List[String]]("json_paths")

  // init actor system, loggers and execution context
  implicit val system: ActorSystem = ActorSystem("TapS3Json")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val standardLogger: LoggingAdapter = Logging(system, clazz)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  // list the bucket contents for the given preffix (if present)
  val s3Contents = S3Source.fromBucket(bucketName, s3Preffix)

  // stream the bucket contents for the provided json paths as singer messages to stdout
  s3Contents
    .log(clazz)
    .map(JsonPaths.asMap(_, jsonPaths))
    .log(clazz)
    .map(SingerAdapter.toSingerRecord)
    .log(clazz)
    .map(SingerAdapter.toJsonString)
    // TODO: instead of writing to stdout a cleaner approach would be using another logger for singer messages
    .runForeach(println(_))
    .onComplete(_ => system.terminate)

}
