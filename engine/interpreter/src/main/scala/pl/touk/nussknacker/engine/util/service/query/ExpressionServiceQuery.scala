package pl.touk.nussknacker.engine.util.service.query

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}
import pl.touk.nussknacker.engine.ModelData
import pl.touk.nussknacker.engine.api.Context
import pl.touk.nussknacker.engine.api.context.ProcessCompilationError.NodeId
import pl.touk.nussknacker.engine.api.context.{PartSubGraphCompilationError, ValidationContext}
import pl.touk.nussknacker.engine.compile.ExpressionCompiler
import pl.touk.nussknacker.engine.expression.ExpressionEvaluator
import pl.touk.nussknacker.engine.graph.evaluatedparam
import pl.touk.nussknacker.engine.graph.expression.Expression
import pl.touk.nussknacker.engine.util.service.query.ServiceQuery.QueryResult
import pl.touk.nussknacker.engine.variables.GlobalVariablesPreparer

import scala.concurrent.{ExecutionContext, Future}

class ExpressionServiceQuery(
                              serviceQuery: ServiceQuery,
                              ctx: Context,
                              expressionEvaluator: ExpressionEvaluator,
                              expressionCompiler: ExpressionCompiler
                            ) {

  import ExpressionServiceQuery._

  def invoke(serviceName: String, args: (String, Expression)*)
            (implicit executionContext: ExecutionContext): Future[QueryResult] = {
    val params = args.map(pair => evaluatedparam.Parameter(pair._1, pair._2)).toList
    invoke(serviceName, params)
  }

  def invoke(serviceName: String,params: List[evaluatedparam.Parameter])
            (implicit executionContext: ExecutionContext): Future[QueryResult] = {
    expressionCompiler.compileValidatedObjectParameters(params, ValidationContext.empty) match {
      case Valid(p) => expressionEvaluator
        .evaluateParameters(p, ctx)(nodeId,ServiceQuery.jobData.metaData, executionContext)
        .flatMap {
          case (_, vars) =>
            serviceQuery
              .invoke(serviceName, vars.toList: _*)
        }
      case Invalid(e) => Future.failed(ParametersCompilationException(e))
    }
  }
}

object ExpressionServiceQuery {
  case class ParametersCompilationException(nel:NonEmptyList[PartSubGraphCompilationError])
    extends IllegalArgumentException(nel.toString())
  private implicit val nodeId: NodeId = NodeId("defaultNodeId")
  private val context = Context("")

  def apply(serviceQuery: ServiceQuery, modelData: ModelData): ExpressionServiceQuery =
    new ExpressionServiceQuery(
      serviceQuery = serviceQuery,
      ctx = context,
      expressionEvaluator = expressionEvaluator(modelData),
      expressionCompiler = expressionCompiler(modelData)
    )

  //TODO: extract shared part with TestInfoProvider
  private def expressionEvaluator(modelData: ModelData): ExpressionEvaluator = {
    ExpressionEvaluator.withoutLazyVals(GlobalVariablesPreparer(modelData.processWithObjectsDefinition.expressionConfig), List.empty)
  }

  private def expressionCompiler(modelData: ModelData) = {
    ExpressionCompiler.withoutOptimization(
      modelData.modelClassLoader.classLoader,
      modelData.dictServices.dictRegistry,
      modelData.processDefinition.expressionConfig,
      modelData.processDefinition.settings
    )
  }
}