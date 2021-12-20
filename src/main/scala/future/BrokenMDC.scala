package future

import logging.Logging
import org.slf4j.MDC

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object BrokenMDC extends App with Logging {
  // simulate one thread per request
  val futures = (1 to 50).map { index =>
    for {
      _ <- Future(MDC.put("correlationId", s"correlation-$index"))
      _ <- runStep1(index)
      _ <- Future(MDC.clear())
    } yield ()
  }
  val results = Future.sequence(futures)
  Await.result(results, 30.seconds)

  private def runStep1(index: Int): Future[Unit] =
    Future {
      logger.info("running step 1 (correlationId should be correlation-{})", index)
      val correlationId = MDC.get("correlationId")
      val expected = s"correlation-$index"
      assert(correlationId == expected, s"$correlationId !== $expected")
      runStep2(index)
    }

  private def runStep2(index: Int): Unit = {
    logger.info("running step 2 (correlationId should be correlation-{})", index)
    assert(MDC.get("correlationId") == s"correlation-$index")
  }
}
