package com.pagantis.singer.taps.it

import akka.Done
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Attributes}
import akka.stream.alpakka.s3.BucketAccess.{AccessGranted, NotExists}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.alpakka.s3.{S3Attributes, S3Settings}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.AwsRegionProvider
import com.pagantis.singer.taps.S3Source
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.{Await, ExecutionContextExecutor, Future}
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
      Await.ready(Http().shutdownAllConnectionPools, 10 seconds)
    }

    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = system.dispatcher
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

    val minioSettings: Attributes =
      S3Attributes.settings(
        S3Settings()
          .withCredentialsProvider(rightCredentialsProvider)
          .withS3RegionProvider(rightRegionProvider)
          .withEndpointUrl(rightEndpoint)
      )

    implicit val defaultPatience: PatienceConfig = PatienceConfig(90.seconds, 30.millis)

    val testBucketMustExist = "test-tap-s3-exists"
    val testBucketMustNotExist = "test-tap-s3-not-exists"

    private def fixtures = {
      implicit val s3Settings: Attributes = S3Attributes.settings(
        S3Settings()
          .withCredentialsProvider(rightCredentialsProvider)
          .withS3RegionProvider(rightRegionProvider)
          .withEndpointUrl(rightEndpoint)
      )

      // create a test bucket
      val testBucketCreated =
        S3.checkIfBucketExists(testBucketMustExist) flatMap {
          case AccessGranted => Future {Done}
          case NotExists => S3.makeBucket(testBucketMustExist)
          case _  =>
            standardLogger.error("Could not setup must-exist bucket. Wrong credentials?")
            sys.exit(1)
        }
      // destroy missing bucket
      val testBucketDestroyed =
        S3.checkIfBucketExists(testBucketMustNotExist) flatMap {
          case AccessGranted => S3.deleteBucket(testBucketMustNotExist)
          case NotExists => Future {Done}
          case _  =>
            standardLogger.error("Could not setup must-exist bucket. Wrong credentials?")
            sys.exit(1)
        }
      // fill buckets with test data
      def uploadString(body: String, key: String) = {
        Source.single(ByteString(body))
          .runWith(
            S3.multipartUpload(testBucketMustExist, key)
              .withAttributes(s3Settings)
          )
      }
      // put single-line file
      val singleLineBody = """{"one" : "line"}"""
      val putSingleLine = testBucketCreated flatMap ( _ => uploadString(singleLineBody, "short/single-liner"))
      // put multi-line file
      val multiLineBody =
        """
          |{"one": "line"}
          |{"two": "lines"}
          |{"three": "lines"}
        """.stripMargin
      val putMultiLineBody = testBucketCreated flatMap ( _ => uploadString(multiLineBody, "short/multi-liner"))
      // put long multiline
      val longJson =
        """{"_id":"5cfa71aa7075d5f43884843a","index":0,"guid":"82a41960-5e85-4b03-ab21-29c64eed2190","isActive":true,"balance":"$2,058.50","picture":"http://placehold.it/32x32","age":39,"eyeColor":"blue","name":"Natalia Washington","gender":"female","company":"EXOSIS","email":"nataliawashington@exosis.com","phone":"+1 (813) 556-3785"}"""
      val longMultilineBody =
        (longJson + "\n") * 500
      val putLongMultilineBody = testBucketCreated flatMap ( _ => uploadString(longMultilineBody, "long/long-multi-liner"))
      // put nested keys
      val putNestedKeys = testBucketCreated flatMap( _ => {
            for {
              _ <- uploadString(singleLineBody, "recursive/long-multi-liner")
              _ <- uploadString(singleLineBody, "recursive/sub-folder/long-multi-liner")
            } yield Done
          }
        )

      val fixtureReady = for {
        _ <- testBucketCreated
        _ <- testBucketDestroyed
        _ <- putSingleLine
        _ <- putMultiLineBody
        _ <- putNestedKeys
      } yield Done

      Await.ready(fixtureReady, Duration.Inf)

    }

    fixtures

    "S3Source" must {

      "fail to start when invalid credentials are provided" in {
        within(50 seconds) {
          new S3Source(testBucketMustExist).object_contents
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
          new S3Source(testBucketMustNotExist).object_contents
            .withAttributes(minioSettings)
            .log("test-inexistent-bucket-name")
            .runWith(TestSink.probe[String]).request(1).expectError()
        }
      }

      "success to start when valid credentials are provided" in {
        within(50 seconds) {
          new S3Source(testBucketMustExist).object_contents
            .withAttributes(minioSettings)
            .runWith(TestSink.probe[String]).request(1).expectNext()
        }
      }

      "success to stream from a bucket even if provided prefix does not exist" in {
        within(50 seconds) {
          new S3Source(testBucketMustExist, Some("madeup-path")).object_contents
            .withAttributes(minioSettings)
            .runWith(TestSink.probe[String]).request(1).expectComplete()
        }
      }

      "success to stream a single-line file from an S3 bucket" in {
        val completable =
          new S3Source(testBucketMustExist, Some("short/single-liner")).object_contents
            .withAttributes(minioSettings)
            .runWith(Sink.seq)

        val listingResult = completable.futureValue
        listingResult.size shouldBe 1
      }

      "success to stream a multiline file from an S3 bucket" in {
        val completable =
          new S3Source(testBucketMustExist, Some("short/multi-liner")).object_contents
            .withAttributes(minioSettings)
            .runWith(Sink.seq)

        val listingResult = completable.futureValue
        listingResult.size shouldBe 3
      }

      "success to stream a mixed single-line / multiline bucket S3 bucket" in {
        val completable =
          new S3Source(testBucketMustExist, Some("short/")).object_contents
            .withAttributes(minioSettings)
            .runWith(Sink.seq)

        val listingResult = completable.futureValue
        listingResult.size shouldBe 4
      }

      "success to stream with a records limit" in {
        val completable =
          new S3Source(testBucketMustExist, Some("short/"), limit = Some(3)).object_contents
            .withAttributes(minioSettings)
            .runWith(Sink.seq)

        val completableNoLimit =
          new S3Source(testBucketMustExist, Some("short/"), limit = Some(0)).object_contents
            .withAttributes(minioSettings)
            .runWith(Sink.seq)

        completable.futureValue.size shouldBe 3
        completableNoLimit.futureValue.size shouldBe 4
      }

      "success to stream a long json file with correct framing" in {
        val completable =
          new S3Source(testBucketMustExist, Some("long-multi-liner")).object_contents
            .withAttributes(minioSettings)
            .runWith(Sink.seq)
        completable.futureValue.size shouldBe 500
      }

      "stream recursively folders and sub-folders" in {
        val completable =
          new S3Source(testBucketMustExist, Some("recursive")).object_contents
            .withAttributes(minioSettings)
            .runWith(Sink.seq)
        completable.futureValue.size shouldBe 2
      }

    }

}
