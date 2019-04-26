package com.pagantis.singer.taps

import akka.NotUsed
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import akka.util.ByteString

object S3Source {

  private def listObjects(bucketName: String, s3Preffix: Option[String]): Source[String, NotUsed] =
    S3.listBucket(bucketName, s3Preffix).map(_.key)

  def fromBucket(bucketName: String, s3Preffix: Option[String]): Source[String, NotUsed] = {

    val objectsToProcess = listObjects(bucketName, s3Preffix)

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

  def fromBucket(bucketName: String):Source[String, NotUsed] =
    fromBucket(bucketName, None)
  def fromBucket(bucketName: String, s3Preffix: String): Source[String, NotUsed] =
    fromBucket(bucketName, Some(s3Preffix))

}
