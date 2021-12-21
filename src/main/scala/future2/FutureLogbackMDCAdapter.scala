package future2

import ch.qos.logback.classic.util.LogbackMDCAdapter

import java.util
import scala.jdk.CollectionConverters._

case class MDCContextReadOnlyMap(original: Map[String, String]) extends util.Map[String, String] {
  override def size(): Int = original.size
  override def isEmpty: Boolean = original.isEmpty
  override def containsKey(key: Any): Boolean = original.contains(key.asInstanceOf[String])
  override def containsValue(value: Any): Boolean = original.values.exists(_ == value)
  override def get(key: Any): String = original.get(key.asInstanceOf[String]).orNull
  override def put(key: String, value: String): String = throw new Exception("read only map")
  override def remove(key: Any): String = throw new Exception("read only map")
  override def putAll(m: util.Map[_ <: String, _ <: String]): Unit = throw new Exception("read only map")
  override def clear(): Unit = throw new Exception("read only map")
  override def keySet(): util.Set[String] = original.keySet.asJava
  override def values(): util.Collection[String] = original.values.asJavaCollection

  override val entrySet: util.Set[util.Map.Entry[String, String]] = {
    new util.Set[util.Map.Entry[String, String]] {
      override def size(): Int = original.size
      override def isEmpty: Boolean = original.isEmpty
      override def contains(o: Any): Boolean = original.contains(o.asInstanceOf[String])
      override def iterator(): util.Iterator[util.Map.Entry[String, String]] =
        original.iterator.map { case (k, v) =>
          new util.Map.Entry[String, String] {
            override def getKey: String = k
            override def getValue: String = v
            override def setValue(value: String): String = throw new Exception("read only map")
          }
        }.asJava
      override def toArray: Array[AnyRef] = original.toArray
      override def toArray[T](a: Array[T with Object]): Array[T with Object] = throw new Exception("read only map")
      override def add(e: util.Map.Entry[String, String]): Boolean = throw new Exception("read only map")
      override def remove(o: Any): Boolean = throw new Exception("read only map")
      override def containsAll(c: util.Collection[_]): Boolean = throw new Exception("read only map")
      override def addAll(c: util.Collection[_ <: util.Map.Entry[String, String]]): Boolean =
        throw new Exception("read only map")
      override def retainAll(c: util.Collection[_]): Boolean = throw new Exception("read only map")
      override def removeAll(c: util.Collection[_]): Boolean = throw new Exception("read only map")
      override def clear(): Unit = throw new Exception("read only map")
    }
  }
}

class FutureLogbackMDCAdapter(mdcContext: ThreadLocal[Map[String, String]]) extends LogbackMDCAdapter {
  override def put(key: String, `val`: String): Unit =
    mdcContext.set(mdcContext.get() + ((key, `val`)))

  override def remove(key: String): Unit =
    mdcContext.set(mdcContext.get() - key)

  override def clear(): Unit = mdcContext.set(Map.empty)

  override def get(key: String): String =
    mdcContext.get().getOrElse(key, null)

  override def getPropertyMap: util.Map[String, String] =
    mdcContext.get().asJava

  override def getKeys: util.Set[String] =
    mdcContext.get().keySet.asJava

  override def getCopyOfContextMap: util.Map[String, String] = new MDCContextReadOnlyMap(mdcContext.get())

  override def setContextMap(contextMap: util.Map[String, String]): Unit =
    contextMap match {
      case MDCContextReadOnlyMap(values) => mdcContext.set(values)
      case _                             => mdcContext.set(contextMap.asScala.toMap)
    }
}

object FutureLogbackMDCAdapter {
  private val MDCContext: ThreadLocal[Map[String, String]] =
    ThreadLocal.withInitial[Map[String, String]](() => Map.empty)

  def initialize(): Unit = {
    import org.slf4j.MDC
    val field = classOf[MDC].getDeclaredField("mdcAdapter")
    field.setAccessible(true)
    field.set(null, new FutureLogbackMDCAdapter(MDCContext))
    field.setAccessible(false)
  }
}
