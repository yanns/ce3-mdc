package simplemdc

import logging.Logging
import org.slf4j.MDC

object SimpleMDC extends App with Logging {
  // simulate one thread per request
  val threads = (1 to 50).map { index =>
    val thread = new Thread(() => {
      MDC.put("correlationId", s"correlation-$index")
      runStep1(index)
      MDC.clear()
    })
    thread.start()
    thread
  }
  threads.foreach(_.join())

  private def runStep1(index: Int): Unit = {
    logger.info("running step 1 (correlationId should be correlation-{})", index)
    runStep2(index)
  }

  private def runStep2(index: Int): Unit =
    logger.info("running step 2 (correlationId should be correlation-{})", index)
}
