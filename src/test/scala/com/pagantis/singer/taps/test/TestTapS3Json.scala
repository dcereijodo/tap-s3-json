package com.pagantis.singer.taps.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import com.pagantis.singer.taps.S3Source
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// source docs for creating and understanding this test h
// https://doc.akka.io/docs/akka/current/testing.html#testing-actor-systems
// https://doc.akka.io/docs/akka/current/stream/stream-testkit.html

class TestTapS3Json
  extends TestKit(ActorSystem("TestTapS3Json"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val materializer = ActorMaterializer()

  "S3Source" must {
    "fail to start when no valid credentials are provided" in {
      within(50 seconds) {
        val invalidCredentialsException =
          S3Source.object_contents.inBucket("some-irrelevant-bucket")
            .runWith(TestSink.probe[String]).request(1).expectError()
        assert(invalidCredentialsException.getClass == classOf[com.amazonaws.SdkClientException])
      }
    }
  }

}
