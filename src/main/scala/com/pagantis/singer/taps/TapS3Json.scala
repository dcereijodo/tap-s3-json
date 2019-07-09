package com.pagantis.singer.taps

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Success

object TapS3Json extends App {

  val clazz = getClass.getName

  // init actor system, loggers and execution context
  implicit val system: ActorSystem = ActorSystem("TapS3Json")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val standardLogger: LoggingAdapter = Logging(system, clazz)
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  val singerAdapter = SingerAdapter.fromConfig

  val startTime = System.nanoTime()

  // stream the bucket contents for the provided json paths as singer messages to stdout
  val tapComputation =
    S3Source.fromConfig.object_contents
    .log(clazz)
    .mapAsync(12)( contentLine => Future{ singerAdapter.toSingerRecord(contentLine._1) })
    .log(clazz)
    .async
    .mapAsync(12)(singerMessage => Future{ singerAdapter.toJsonString(singerMessage)})
    // TODO: instead of writing to stdout a cleaner approach would be using another logger for singer messages
    .runForeach(println(_))

  // Shutdown sequence is fairly. First shut down the underlying http infrastructure
  // and then terminate the materializer and actor system. This is to avoid akka to complaint about abrupt termination
  // as it is described in this issue https://github.com/akka/akka-http/issues/497
  // This shutdown sequence was copied from another related issue: https://github.com/akka/akka-http/issues/907#issuecomment-345288919
  def shutdownSequence =
    Http().shutdownAllConnectionPools

  Await.ready(tapComputation, Duration.Inf)

  standardLogger.info(s"Total execution time: ${(System.nanoTime - startTime)/1000000000} seconds")

  Await.ready(shutdownSequence, Duration.Inf)

  tapComputation.value match {
    case Some(Success(_)) => sys.exit(0)
    case _ => sys.exit(1)
  }

}
