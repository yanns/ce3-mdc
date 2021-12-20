package future

import logging.Logging
import org.slf4j.MDC

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

object BrokenMDC extends App with Logging {
  import scala.concurrent.ExecutionContext.Implicits.global
  FutureMDC.run()
}

object FixedMDC extends App with Logging {
  implicit val ec: ExecutionContext = MDCPropagatorExecutionContext(scala.concurrent.ExecutionContext.global)
  FutureMDC.run()
}

object FutureMDC extends Logging {
  def run()(implicit ec: ExecutionContext): Unit = {
    // simulate one thread per request
    val futures = (1 to 50).map { index =>
      MDC.put("correlationId", s"correlation-$index")
      runStep1(index).map { _ => MDC.clear() }
    }
    val results = Future.sequence(futures)
    Await.result(results, 30.seconds)
  }

  private def runStep1(index: Int)(implicit ec: ExecutionContext): Future[Unit] =
    Future {
      logger.info("running step 1 (correlationId should be correlation-{})", index)
      val correlationId = MDC.get("correlationId")
      val expected = s"correlation-$index"
      assert(correlationId == expected, s"$correlationId !== $expected")
    }.map(_ => runStep2(index))

  private def runStep2(index: Int): Unit = {
    logger.info("running step 2 (correlationId should be correlation-{})", index)
    assert(MDC.get("correlationId") == s"correlation-$index")
  }
}
