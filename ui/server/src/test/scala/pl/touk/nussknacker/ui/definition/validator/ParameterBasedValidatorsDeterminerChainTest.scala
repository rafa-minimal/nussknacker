package pl.touk.nussknacker.ui.definition.validator

import org.scalatest.{FunSuite, Matchers}
import pl.touk.nussknacker.engine.api.definition.{MandatoryValueValidator, NotBlankValueValidator, Parameter}
import pl.touk.nussknacker.engine.api.process.ParameterConfig

class ParameterBasedValidatorsDeterminerChainTest extends FunSuite with Matchers {

  test("determine validators based on parameter") {
    val param = Parameter[String]("param")
    val config = ParameterConfig.empty

    val validators = ParameterValidatorsDeterminerChain(config).determineValidators(param)

    validators shouldBe List(MandatoryValueValidator)
  }

  test("determine validators based on config") {
    val param = Parameter.optional[String]("param")
    val config = ParameterConfig(None, None, Some(List(MandatoryValueValidator, NotBlankValueValidator)))

    val validators = ParameterValidatorsDeterminerChain(config).determineValidators(param)

    validators shouldBe List(MandatoryValueValidator, NotBlankValueValidator)
  }

  test("override validators based on annotation with those from config") {
    val param = Parameter[String]("param")
    val config = ParameterConfig(None, None, Some(List.empty))

    val validators = ParameterValidatorsDeterminerChain(config).determineValidators(param)

    validators shouldBe List.empty
  }

}
