# SLF4J Mapped Diagnostic Context (MDC) with cats-effect 3

## The Mapped Diagnostic Context (MDC)

The logging library Logback provides a very convenient feature: the [Mapped Diagnostic Context (MDC)](http://logback.qos.ch/manual/mdc.html).
This context can be used to store values that can be displayed in every logging statement.

## Simple usage of MDC using one thread per request

[run SimpleMDC](src/main/scala/simplemdc/SimpleMDC.scala)

For example, if we want to trace all log entries for the same request, we can use a `correlationId` variable:

```
MDC.put("correlationId", <unique value for one request>)
runStep1(index)
MDC.clear()
```

Logback must be configured to display the `correlationId` value:

```
<pattern>[%thread] [%X{correlationId}] %-5level %logger{40} - %msg%n</pattern>
```

When logging
```
  private def runStep1(index: Int): Unit = {
    logger.info("running step 1")
    runStep2(index)
  }

  private def runStep2(index: Int): Unit =
    logger.info("running step 2")
```

In the log file, the MDC value for `correlationId` is now printed out:
```
[Thread-17] [correlation-1] INFO  simplemdc.SimpleMDC$ - running step 1 (correlationId should be correlation-1)
[Thread-26] [correlation-10] INFO  simplemdc.SimpleMDC$ - running step 1 (correlationId should be correlation-10)
[Thread-22] [correlation-6] INFO  simplemdc.SimpleMDC$ - running step 1 (correlationId should be correlation-6)
[Thread-17] [correlation-1] INFO  simplemdc.SimpleMDC$ - running step 2 (correlationId should be correlation-1)
[Thread-26] [correlation-10] INFO  simplemdc.SimpleMDC$ - running step 2 (correlationId should be correlation-10)
```

The MDC uses thread-local variable to ensure that each thread has its own value.

## MDC and asynchronous processing

To achieve better scalability, we use n-m, where n tasks are running on m threads.
As one task is not found to one thread anymore, MDC does not work anymore.

[See BrokenMDC](src/main/scala/future/FutureMDC.scala)

We can fix that by using a special execution context that is constantly saving and restoring the MDC:

[See FixedMDC](src/main/scala/future/FutureMDC.scala)

http://yanns.github.io/blog/2014/05/04/slf4j-mapped-diagnostic-context-mdc-with-play-framework/

The default `LogbackMDCAdapter` creates a new `Map` for each execution.
This has an effect on allocations:

![Allocations of Map](MDC%20Future%20allocations.png "Allocations")

and on performances:

![Profiling showing new instantiations of Map](MDC%20Future%20profiling.png "Performances")

To fix that, we can use our [own `LogbackMDCAdapter`, backed by an immutable scala Map](src/main/scala/future2/FutureLogbackMDCAdapter.scala).
