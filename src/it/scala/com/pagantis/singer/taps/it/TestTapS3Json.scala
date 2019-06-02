package com.pagantis.singer.taps.it

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.alpakka.s3.{S3Attributes, S3Settings}
import akka.stream.scaladsl.Sink
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.AwsRegionProvider
import com.pagantis.singer.taps.S3Source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

// source docs for creating and understanding this test
// https://doc.akka.io/docs/akka/current/testing.html#testing-actor-systems
// https://doc.akka.io/docs/akka/current/stream/stream-testkit.html

// the alpakka integration spec is an interesting example of how to create more detailed tests using the s3 connector
// and nio
// https://github.com/akka/alpakka/blob/master/s3/src/test/scala/akka/stream/alpakka/s3/scaladsl/S3IntegrationSpec.scala

// this is an integration test, so its disabled by default
// for running this you need an S3 testing environment, you can use https://docs.min.io/docs/aws-cli-with-minio
// either on their public service or setting up a local environment with docker https://github.com/minio/minio

//noinspection ScalaUnusedSymbol
class TestTapS3Json
  extends TestKit(ActorSystem("TestTapS3Json"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll
  {

    override def afterAll: Unit = {
      implicit val ec: ExecutionContextExecutor = system.dispatcher
      Http().shutdownAllConnectionPools.andThen { case _ =>
        materializer.shutdown
        TestKit.shutdownActorSystem(system)
      }
    }

    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val standardLogger: LoggingAdapter = Logging(system, getClass.getName)

    val wrongBasicCredentials = new BasicAWSCredentials("WRONGKEY", "WRONGSECRET")
    // credentials for accessing public minio service
    // https://docs.min.io/docs/aws-cli-with-minio
    val rightBasicCredentials = new BasicAWSCredentials("Q3AM3UQ867SPQQA43P2F", "zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG")
    val rightRegion = "us-east-1"
    val rightEndpoint = "https://play.min.io:9000"

    val wrongCredentialsProvider = new AWSStaticCredentialsProvider(wrongBasicCredentials)
    val rightCredentialsProvider = new AWSStaticCredentialsProvider(rightBasicCredentials)

    val rightRegionProvider: AwsRegionProvider = new AwsRegionProvider {
      override def getRegion: String = rightRegion
    }

    implicit val defaultPatience: PatienceConfig = PatienceConfig(90.seconds, 30.millis)

    "S3Source" must {
      "fail to start when invalid credentials are provided" in {
        within(50 seconds) {
          new S3Source("bateboiko").object_contents // this is random bucket at minio public server https://play.min.io:9000/minio/00test/
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(wrongCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .log("test-invalid-credentials")
            .runWith(TestSink.probe[String]).request(1).expectError()
        }
      }

      "fail when provided bucket name does not exist" in {
        within(50 seconds) {
          new S3Source("some-irrelevant-bucket").object_contents
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .log("test-inexistent-bucket-name")
            .runWith(TestSink.probe[String]).request(1).expectError()
        }
      }

      "success to start when valid credentials are provided" in {
        within(50 seconds) {
          new S3Source("183p1xn63oohgdtu3jcposeygupg5mdc").object_contents // this is random bucket at minio public server https://play.min.io:9000/minio/
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .runWith(TestSink.probe[String]).request(1).expectComplete()
        }
      }

      "success to stream from a bucket even if provided preffix does not exist" in {
        within(50 seconds) {
          new S3Source("183p1xn63oohgdtu3jcposeygupg5mdc", Some("madeup-path")).object_contents // this is random bucket at minio public server https://play.min.io:9000/minio/
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .runWith(TestSink.probe[String]).request(1).expectComplete()
        }
      }

      "success to stream a single-line file from an S3 bucket" in {
        val completable =
          // this is bucket with test data at minio public server https://play.min.io:9000/minio/tap-s3-json/
          new S3Source("tap-s3-json", Some("single-line-file")).object_contents
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .runWith(Sink.seq)

        val listingResult = completable.futureValue
        listingResult.size shouldBe 1
      }

      "success to stream a multiline file from an S3 bucket" in {
        val completable =
          // this is bucket with test data at minio public server https://play.min.io:9000/minio/tap-s3-json/
          new S3Source("tap-s3-json", Some("multi-line-file")).object_contents
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .runWith(Sink.seq)

        val listingResult = completable.futureValue
        listingResult.size shouldBe 3
      }

      "success to stream a mixed single-line / multiline bucket S3 bucket" in {
        val completable =
          // this is bucket with test data at minio public server https://play.min.io:9000/minio/tap-s3-json/
          new S3Source("tap-s3-json").object_contents
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .runWith(Sink.seq)

        val listingResult = completable.futureValue
        listingResult.size shouldBe 4
      }
      "success to stream with a records limit" in {
        val completable =
          // this is bucket with test data at minio public server https://play.min.io:9000/minio/tap-s3-json/
          new S3Source("tap-s3-json", limit = Some(3)).object_contents
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .runWith(Sink.seq)

        val completableNoLimit =
        // this is bucket with test data at minio public server https://play.min.io:9000/minio/tap-s3-json/
          new S3Source("tap-s3-json", limit = Some(0)).object_contents
            .withAttributes(
              S3Attributes.settings(
                S3Settings()
                  .withCredentialsProvider(rightCredentialsProvider)
                  .withS3RegionProvider(rightRegionProvider)
                  .withEndpointUrl(rightEndpoint)
              )
            )
            .runWith(Sink.seq)

        completable.futureValue.size shouldBe 3
        completableNoLimit.futureValue.size shouldBe 4
      }
    }

}
