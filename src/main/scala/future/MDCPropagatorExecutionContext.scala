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
    val context = getCopyOfContext()

    def execute(r: Runnable) = self.execute(propagatingRunnable(context, r))
    def reportFailure(t: Throwable) = self.reportFailure(t)
  }

  private def getCopyOfContext(): Context = MDC.getCopyOfContextMap

  private def propagatingRunnable(context: Context, r: Runnable): Runnable =
    () => withContext(context)(r.run())

  private def withContext[R](context: Context)(fn: => R): Unit = {
    // backup the callee context
    val oldMDCContext = getCopyOfContext()

    // Run the runnable with the captured context
    setContext(context)
    try fn
    finally
    // restore the callee MDC context
    setContext(oldMDCContext)
  }

  private def setContext(context: Context): Unit =
    if (context == null) {
      MDC.clear()
    } else {
      MDC.setContextMap(context)
    }
}

object MDCPropagatorExecutionContext {
  def apply(delegate: ExecutionContext): MDCPropagatorExecutionContext =
    new ExecutionContext with MDCPropagatorExecutionContext {
      override def reportFailure(cause: Throwable): Unit = delegate.reportFailure(cause)
      override def execute(runnable: Runnable): Unit = delegate.execute(runnable)
    }
}
