package pl.touk.nussknacker.engine.api.context

import cats.data.ValidatedNel

/**
  * Wrapper for tuple of definition and implementation of variable context transformation
  * @param definition Definition of variable context transformation - defines how will look ValidationContext
  *                   (types of variables) after transformation in runtime
  * @param implementation Implements real variable context transformation which was defined in definition
  *                       Returned type depends on execution engine. It should be lazy evaluated to make sure that
  *                       none runtime work will be run in compilation/validation stage
  */
case class ContextTransformation(definition: ContextTransformationDef,
                                 implementation: Any) extends AbstractContextTransformation {
  override type ContextTransformationDefType = ContextTransformationDef
}

case class JoinContextTransformation(definition: JoinContextTransformationDef,
                                     implementation: Any) extends AbstractContextTransformation {
  override type ContextTransformationDefType = JoinContextTransformationDef
}

sealed trait AbstractContextTransformation {

  type ContextTransformationDefType <: AbstractContextTransformationDef

  def definition: ContextTransformationDefType

  // Should be lazy evaluated to be sure that none runtime work will be run in compilation/validation stage
  // The result of evaluation depends on execution engine
  def implementation: Any

}

/**
  * Set of builders for ContextTransformation e.g.
  * `
  *   ContextTransformation
  *     .definedBy(_.withVariable("foo", Typed[String])
  *     .implementedBy { () =>
  *       Future.success(Context("").withVariable("foo", "bar")
  *     }
  * `
  */
object ContextTransformation {

  def join: JoinBuilder = new JoinBuilder

  def definedBy(definition: ContextTransformationDef): DefinedByBuilder =
    new DefinedByBuilder(definition)

  def definedBy(transformContext: ValidationContext => ValidatedNel[ProcessCompilationError, ValidationContext]): DefinedByBuilder =
    new DefinedByBuilder(new ContextTransformationDef {
      override def transform(context: ValidationContext): ValidatedNel[ProcessCompilationError, ValidationContext] =
        transformContext(context)
    })

  class DefinedByBuilder(definition: ContextTransformationDef) {
    def implementedBy(impl: Any): ContextTransformation =
      ContextTransformation(definition, impl)
  }

  class JoinBuilder {
    def definedBy(definition: JoinContextTransformationDef): JoinDefinedByBuilder =
      new JoinDefinedByBuilder(definition)

    def definedBy(transformContexts: Map[String, ValidationContext] => ValidatedNel[ProcessCompilationError, ValidationContext]): JoinDefinedByBuilder =
      new JoinDefinedByBuilder(new JoinContextTransformationDef {
        override def transform(contextByBranchId: Map[String, ValidationContext]): ValidatedNel[ProcessCompilationError, ValidationContext] =
          transformContexts(contextByBranchId)
      })
  }

  class JoinDefinedByBuilder(definition: JoinContextTransformationDef) {
    def implementedBy(impl: Any): JoinContextTransformation =
      JoinContextTransformation(definition, impl)
  }

}

sealed trait AbstractContextTransformationDef {

  type InputContext

  def transform(context: InputContext): ValidatedNel[ProcessCompilationError, ValidationContext]

}

trait ContextTransformationDef extends AbstractContextTransformationDef {

  override final type InputContext = ValidationContext

  def andThen(nextTransformation: ContextTransformationDef): ContextTransformationDef =
    new ContextTransformationDef {
      override def transform(context: ValidationContext): ValidatedNel[ProcessCompilationError, ValidationContext] =
        ContextTransformationDef.this.transform(context).andThen(nextTransformation.transform)
    }

}

trait JoinContextTransformationDef extends AbstractContextTransformationDef {

  // branchId -> ValidationContext
  override final type InputContext = Map[String, ValidationContext]

  def andThen(nextTransformation: ContextTransformationDef): JoinContextTransformationDef =
    new JoinContextTransformationDef {
      override def transform(contextByBranchId: Map[String, ValidationContext]): ValidatedNel[ProcessCompilationError, ValidationContext] =
        JoinContextTransformationDef.this.transform(contextByBranchId).andThen(nextTransformation.transform)
    }

}