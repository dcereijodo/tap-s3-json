package akka.stream.impl

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.util.ByteString

import scala.util.control.NonFatal

class JsonFramingStage[Ctx](maximumObjectLength: Int) extends GraphStage[FlowShape[(ByteString, Ctx), (ByteString, Ctx)]] {

  val in = Inlet[(ByteString, Ctx)]("JsonFramingWithContext.in")
  val out = Outlet[(ByteString, Ctx)]("JsonFramingWithContext.out")

  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      private val buffer = new JsonObjectParser(maximumObjectLength)
      private var ctx: Option[Ctx] = None

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val (jsonChunk, context) = grab(in)
          buffer.offer(jsonChunk)
          ctx = Some(context)
          tryPopBuffer()
        }
        override def onUpstreamFinish(): Unit = {
          buffer.poll() match {
            case Some(json) => {
              emit(out, (json, ctx.get))
            }
            case _          => completeStage()
          }
        }
      })

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          tryPopBuffer()
        }
      })

      def tryPopBuffer() = {
        try buffer.poll() match {
          case Some(json) => push(out, (json, ctx.get))
          case _          => if (isClosed(in)) completeStage() else pull(in)
        } catch {
          case NonFatal(ex) => failStage(ex)
        }
      }
    }

}

object JsonFramingStage {
  def objectScanner[Ctx](maximumObjectLength: Int): Flow[(ByteString,Ctx), (ByteString,Ctx), NotUsed] = {
    Flow[(ByteString, Ctx)].via(
      new JsonFramingStage[Ctx](maximumObjectLength)
    )
  }
}