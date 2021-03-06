package pl.touk.nussknacker.engine.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field/method annotated with @Hidden won't appear in code completion and cannot be used in (spel) expressions
 *
 * <b>Please not that for scala fields/case class parameters you have to use @(Hidden @getter)</b>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Hidden {
}
