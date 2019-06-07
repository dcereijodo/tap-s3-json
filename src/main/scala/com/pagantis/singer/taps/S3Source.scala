package com.pagantis.singer.taps

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{Framing, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

object S3Source extends PartitioningUtils {

  def fromConfig = {
    val config = ConfigFactory.load().getConfig("tap")
    import net.ceedubs.ficus.Ficus._
    new S3Source(
      config.getString("bucket_name"),
      config.as[Option[String]]("prefix"),
      buildPartitioningSubPath(
        config.as[Option[String]]("partitioning.key"),
        config.as[Option[String]]("partitioning.value")),
      config.as[Option[Long]]("limit"),
      config.as[Int]("frame_length")
    )
  }

}

class S3Source(
                bucketName: String,
                s3Preffix: Option[String] =  None,
                partitioningSubPath: Option[String] = None,
                limit: Option[Long] = None,
                maximumFrameLength: Int = 1024*4
              )
extends PartitioningUtils
{
  val clazz = getClass.getName

  def object_keys: Source[String, NotUsed] =
    S3.listBucket(bucketName, buildS3Preffix(s3Preffix, partitioningSubPath)).map(_.key)

  def object_contents: Source[String, NotUsed] = {

    val objectsToProcess = object_keys

    val contents = objectsToProcess
      .flatMapConcat(objectHandler =>
        S3
          .download(bucketName, objectHandler)
          .collect { // take successful downloads
            case Some(successfulDownloadAsASource) =>

              // first element in the tuple contains the actual source, second element is metadata
              successfulDownloadAsASource._1
          }
          .flatMapConcat(p => p ++ Source(List(ByteString("\n"))))
      )
      .log(clazz)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = maximumFrameLength, allowTruncation = false))
      .log(clazz)
      .map(_.utf8String)

    limit match {
      case Some(max) if max > 0 => contents take max
      case _ => contents
    }
  }

}
