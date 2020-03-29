package pl.touk.nussknacker.engine.flink.api.process

import org.apache.flink.api.common.functions.RuntimeContext
import org.apache.flink.streaming.api.datastream.DataStreamSink
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.scala._
import pl.touk.nussknacker.engine.api.{InterpretationResult, LazyParameterInterpreter}
import pl.touk.nussknacker.engine.api.process.Sink

trait FlinkSink extends Sink {

  def registerSink(dataStream: DataStream[InterpretationResult],
                   lazyParameterFunctionHelper: FlinkLazyParameterFunctionHelper): DataStreamSink[_]

}

trait BasicFlinkSink extends FlinkSink {

  override def registerSink(dataStream: DataStream[InterpretationResult],
                            lazyParameterFunctionHelper: FlinkLazyParameterFunctionHelper): DataStreamSink[_] = {
    dataStream.map(_.output).addSink(toFlinkFunction)
  }

  def toFlinkFunction: SinkFunction[Any]

}
