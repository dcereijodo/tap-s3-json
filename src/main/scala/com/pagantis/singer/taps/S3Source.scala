package com.pagantis.singer.taps

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
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
        config.as[Option[String]]("partitioning.value"))
    )
  }

}

class S3Source(
                bucketName: String,
                s3Preffix: Option[String] =  None,
                partitioningSubPath: Option[String] = None
              )
extends PartitioningUtils
{

  def object_keys: Source[String, NotUsed] =
    S3.listBucket(bucketName, buildS3Preffix(s3Preffix, partitioningSubPath)).map(_.key)

  def object_contents: Source[String, NotUsed] = {

    val objectsToProcess = object_keys

    objectsToProcess
      .flatMapConcat(objectHandler =>
        S3
          .download(bucketName, objectHandler)
          .collect { // take successful downloads
            case Some(successfulDownloadAsASource) =>
              // first element in the tuple contains the actual source, second element is metadata
              successfulDownloadAsASource._1
          }
          .flatMapConcat(p => p)
      )
      .mapConcat(_.utf8String.split("\n").toList)
  }

}
