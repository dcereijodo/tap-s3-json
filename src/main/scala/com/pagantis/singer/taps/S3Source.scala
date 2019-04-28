package com.pagantis.singer.taps

import akka.NotUsed
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source

object S3Source {

  private object object_keys {
    def inBucket(bucketName: String, s3Preffix: Option[String]): Source[String, NotUsed] =
      S3.listBucket(bucketName, s3Preffix).map(_.key)
  }

  object object_contents {

    def inBucket(bucketName: String, s3Preffix: Option[String]): Source[String, NotUsed] = {

      val objectsToProcess = object_keys.inBucket(bucketName, s3Preffix)

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
        .map(_.utf8String)
    }

    def inBucket(bucketName: String):Source[String, NotUsed] =
      inBucket(bucketName, None)
    def inBucket(bucketName: String, s3Preffix: String): Source[String, NotUsed] =
      inBucket(bucketName, Some(s3Preffix))
  }

}
