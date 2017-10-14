package me.shils.util.logging

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import org.codehaus.groovy.transform.LogASTTransformation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Identical to {@link groovy.util.logging.Log}, with the addition of supporting logging within enum constructors.
 *
 * @author Shil Sinha
 */
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.TYPE])
@GroovyASTTransformationClass("org.codehaus.groovy.transform.LogASTTransformation")
@interface Log {
  String value() default "log"
  String category() default "DUMMY_CATEGORY"
  Class<? extends LogASTTransformation.LoggingStrategy> loggingStrategy() default JavaUtilLoggingStrategy

  @InheritConstructors
  static class JavaUtilLoggingStrategy extends groovy.util.logging.Log.JavaUtilLoggingStrategy implements EnumConstructorLoggingStrategy {
  }
}