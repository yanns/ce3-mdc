package future

import org.slf4j.MDC

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/** propagates the logback MDC in future callbacks
  */
trait MDCPropagatorExecutionContext extends ExecutionContextExecutor {
  self =>

  type Context = java.util.Map[String, String]

  override def prepare(): ExecutionContext = new ExecutionContext {
    // capture the MDC
    val context = MDC.getCopyOfContextMap()

    def execute(r: Runnable): Unit =
      self.execute(() => {
        // Run the runnable with the captured context
        setContext(context)
        r.run()
      })

    def reportFailure(t: Throwable): Unit = self.reportFailure(t)
  }

  private def setContext(context: Context): Unit =
    if (context == null) MDC.clear()
    else MDC.setContextMap(context)
}

object MDCPropagatorExecutionContext {
  def apply(delegate: ExecutionContext): MDCPropagatorExecutionContext =
    new ExecutionContext with MDCPropagatorExecutionContext {
      override def reportFailure(cause: Throwable): Unit = delegate.reportFailure(cause)
      override def execute(runnable: Runnable): Unit = delegate.execute(runnable)
    }
}
