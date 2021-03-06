package pl.touk.nussknacker.engine.management.sample.source

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor
import org.apache.flink.streaming.api.windowing.time.Time
import pl.touk.nussknacker.engine.api.process.TestDataParserProvider
import pl.touk.nussknacker.engine.api.test.{NewLineSplittedTestDataParser, TestDataParser}
import pl.touk.nussknacker.engine.flink.api.process.BasicFlinkSource
import org.apache.flink.streaming.api.scala._

//this not ending source is more reliable in tests than CollectionSource, which terminates quickly
class NoEndingSource extends BasicFlinkSource[String] with TestDataParserProvider[String] {
  override val typeInformation: TypeInformation[String] = implicitly[TypeInformation[String]]

  override def timestampAssigner = Option(new BoundedOutOfOrdernessTimestampExtractor[String](Time.minutes(10)) {
    override def extractTimestamp(element: String): Long = System.currentTimeMillis()
  })

  override def testDataParser: TestDataParser[String] = new NewLineSplittedTestDataParser[String] {
    override def parseElement(testElement: String): String = testElement
  }

  override def flinkSourceFunction: SourceFunction[String] = new SourceFunction[String] {
    var running = true
    var counter = new AtomicLong()
    val afterFirstRun = new AtomicBoolean(false)

    override def cancel(): Unit = {
      running = false
    }

    override def run(ctx: SourceContext[String]): Unit = {
      val r = new scala.util.Random
      while (running) {
        if (afterFirstRun.getAndSet(true)) {
          ctx.collect("TestInput" + r.nextInt(10))
        } else {
          ctx.collect("TestInput1")
        }
        Thread.sleep(2000)
      }
    }
  }
}
