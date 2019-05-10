package com.pagantis.singer.taps

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Success

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

  // stream the bucket contents for the provided json paths as singer messages to stdout
  S3Source.object_contents.inBucket(bucketName, s3Preffix)
    .log(clazz)
    .map(JsonPaths.asMap(_, jsonPaths))
    .log(clazz)
    .map(SingerAdapter.toSingerRecord)
    .log(clazz)
    .map(SingerAdapter.toJsonString)
    // TODO: instead of writing to stdout a cleaner approach would be using another logger for singer messages
    .runForeach(println(_))
    // Now comes a fairly complicated shutdown sequence, which first shuts down the underlying http infrastructure
    // and then terminates the materializer and actor system. This is to avoid akka to complaint about abrupt termination
    // as it is described in this issue https://github.com/akka/akka-http/issues/497
    // This shutdown sequence was copied from another related issue: https://github.com/akka/akka-http/issues/907#issuecomment-345288919
    .onComplete(res => {
        Http().shutdownAllConnectionPools.andThen { case _ =>
          materializer.shutdown
          system.terminate
        }
        res match {
          case Success(_) => sys.exit(0)
          case _ => sys.exit(1)
        }
      }
    )

  // block main thread
  // exit will be handled on the stream exit callback
  Await.ready(Future.never, Duration.Inf)
}
