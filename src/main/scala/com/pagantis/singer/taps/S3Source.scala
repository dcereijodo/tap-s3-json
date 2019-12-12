package com.pagantis.singer.taps

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{Balance, Flow, Framing, GraphDSL, Merge, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

case class ObjectMetadata(key: String, version: Option[String], lastModifiedAt: String)

/**
  * Factory of S3Source
  */
object S3Source extends ObjectKeyUtils {

  def fromConfig: S3Source = {
    val config = ConfigFactory.load().getConfig("tap")
    import net.ceedubs.ficus.Ficus._
    new S3Source(
      config.getString("bucket_name"),
      config.as[Option[String]]("prefix"),
      buildPartitioningSubPath(
        config.as[Option[String]]("partitioning.key"),
        config.as[Option[String]]("partitioning.value")),
      config.as[Option[Long]]("limit"),
      config.as[Option[String]]("filtered_with"),
      config.as[Int]("frame_length"),
      config.as[Int]("worker_count")
    )
  }

}

/**
  * A `S3Source` implements the construction of Akka streams of S3 object keys and contents
  * @param bucketName The name of the bucket to stream from
  * @param s3Prefix The prefix of the S3 objects to be streamed. If it's [[None]] the whole bucket is streamed.
  * @param partitioningSubPath The partitioning sub-path to be used if any.
  * @param limit The number of elements to be streamed. If it's [[None]] or 0 no limit is applied.
  * @param filteredWith A regular expression to further filter S3 object keys. Is an expression is provided, only
  *                     matching keys are streamed.
  * @param maximumFrameLength The maximum size of the buffer used for [[akka.stream.scaladsl.Framing]] object contents.
  * @param workerCount Total number of workers to be used for the download.
  */
class S3Source(
                bucketName: String,
                s3Prefix: Option[String] =  None,
                partitioningSubPath: Option[String] = None,
                limit: Option[Long] = None,
                filteredWith: Option[String] = None,
                maximumFrameLength: Int = 1024*4,
                workerCount: Int = 10
              )
extends ObjectKeyUtils
{

  /**
    * Returns a [[Source]] of object keys for the provided settings.
    */
  def object_keys: Source[String, NotUsed] =
    filteredWith match {
      case Some(filteringExpression) =>
        S3.listBucket(bucketName, buildS3Prefix(s3Prefix, partitioningSubPath))
          .map(_.key)
          .filter(filterKey(_,filteringExpression))
      case None =>
        S3.listBucket(bucketName, buildS3Prefix(s3Prefix, partitioningSubPath)).map(_.key)
    }

  /**
    * Returns a [[Source]] of tuples (object metadata, object contents) for the provided settings.
    * The object contents are provided as text, and the object metadata are wrapped in a [[ObjectMetadata]] class.
    */
  def object_contents: Source[(String, ObjectMetadata), NotUsed] = {

    import GraphDSL.Implicits._

    val objectsToProcess = object_keys

    // a flow that maps S3 object keys to a tuple (contents, metadata)
    val keyFlow: Flow[String, (ByteString, ObjectMetadata), NotUsed] = Flow[String].map(
      key =>
        S3
          .download(bucketName, key)
          .collect { // take successful downloads
            // first element in the tuple contains the actual byte string as a source, second element is metadata
            case Some((bytestream, metadata)) =>

              val context = ObjectMetadata(
                key,
                metadata.versionId,
                metadata.lastModified.toIsoDateTimeString
              )
              bytestream.via(Framing.delimiter(ByteString("\n"), maximumFrameLength, allowTruncation = true)).map((_, context))
          }
          .flatMapConcat(p => p)
      ).flatMapConcat(p => p)

    val parallelDownload: Flow[String, (ByteString, ObjectMetadata), NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit builder =>

      val balancer = builder.add(Balance[String](workerCount, waitForAllDownstreams = true))
      val merge = builder.add(Merge[(ByteString, ObjectMetadata)](workerCount))

      for (_ <- 1 to workerCount) {
        // for each worker, add an edge from the balancer to the worker, then wire
        // it to the merge element
        balancer ~> keyFlow.async ~> merge
      }

      FlowShape(balancer.in, merge.out)
    })

    val contents =
      objectsToProcess
        .via(parallelDownload)
        .map{case (contents, key) => (contents.utf8String, key)}

    limit match {
      case Some(max) if max > 0 => contents take max
      case _ => contents
    }
  }

}
