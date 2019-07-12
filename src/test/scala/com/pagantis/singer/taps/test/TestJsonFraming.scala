package com.pagantis.singer.taps.test

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.impl.JsonFramingWithContext
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures

class TestJsonFraming extends TestKit(ActorSystem("TestTapS3Json"))   with ImplicitSender
  with WordSpecLike
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "A JsonFramingStage" must {
    "frame one single-chucked JSON" in {
      Source(
        List(
          (ByteString("""{"somekey":"someval"}"""),"object1")
        )
      ).via(JsonFramingWithContext.objectScanner(2000))
        .runWith(TestSink.probe[(ByteString,String)])
        .request(1)
        .expectNext((ByteString("""{"somekey":"someval"}"""), "object1"))
        .expectComplete()
    }
    "frame two single-chucked JSON" in {
      Source(
        List(
          (ByteString("""{"somekey":"someval"}"""),"object1"),
          (ByteString("""{"somekey2":"someval"}"""),"object2")
        )
      ).via(JsonFramingWithContext.objectScanner(2000))
        .runWith(TestSink.probe[(ByteString,String)])
        .request(2)
        .expectNext((ByteString("""{"somekey":"someval"}"""), "object1"))
        .expectNext((ByteString("""{"somekey2":"someval"}"""), "object2"))
        .expectComplete()
    }
    "frame one multi-chucked JSON" in {
      Source(
        List(
          (ByteString("""{"somekey":"""),"object1"),
          (ByteString(""""someval"}"""),"object1")
        )
      ).via(JsonFramingWithContext.objectScanner(2000))
        .runWith(TestSink.probe[(ByteString,String)])
        .request(1)
        .expectNext((ByteString("""{"somekey":"someval"}"""), "object1"))
        .expectComplete()
    }
    "frame two multi-chucked JSON" in {
      Source(
        List(
          (ByteString("""{"somekey":"""),"object1"),
          (ByteString(""""someval"}"""),"object1"),
          (ByteString("""{"somekey2":"""),"object2"),
          (ByteString(""""someval"}"""),"object2")
        )
      ).via(JsonFramingWithContext.objectScanner(2000))
        .runWith(TestSink.probe[(ByteString,String)])
        .request(2)
        .expectNext((ByteString("""{"somekey":"someval"}"""), "object1"))
        .expectNext((ByteString("""{"somekey2":"someval"}"""), "object2"))
        .expectComplete()
    }
  }

}
