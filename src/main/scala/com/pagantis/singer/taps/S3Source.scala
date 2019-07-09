package com.pagantis.singer.taps

import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.impl.JsonFramingStage
import akka.stream.scaladsl.{Balance, Flow, GraphDSL, Merge, Source}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory

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

  def object_keys: Source[String, NotUsed] =
    filteredWith match {
      case Some(filteringExpression) =>
        S3.listBucket(bucketName, buildS3Prefix(s3Prefix, partitioningSubPath))
          .map(_.key)
          .filter(filterKey(_,filteringExpression))
      case None =>
        S3.listBucket(bucketName, buildS3Prefix(s3Prefix, partitioningSubPath)).map(_.key)
    }


  def object_contents: Source[(String, String), NotUsed] = {

    import GraphDSL.Implicits._

    val objectsToProcess = object_keys
    val keyFlow: Flow[String, (ByteString, String), NotUsed] = Flow[String].map(
      key =>
        S3
          .download(bucketName, key)
          .collect { // take successful downloads
            case Some(successfulDownloadAsASource) =>

              // first element in the tuple contains the actual source, second element is metadata
              successfulDownloadAsASource._1.map((_,key))
          }
          .flatMapConcat(p => p.via(JsonFramingStage.objectScanner(maximumFrameLength)))
      ).flatMapConcat(p => p)

    val parallelDownload: Flow[String, (ByteString, String), NotUsed] = Flow.fromGraph(GraphDSL.create() { implicit builder =>

      val balancer = builder.add(Balance[String](workerCount, waitForAllDownstreams = true))
      val merge = builder.add(Merge[(ByteString, String)](workerCount))

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
