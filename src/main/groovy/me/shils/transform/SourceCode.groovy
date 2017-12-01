package me.shils.transform

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Class level annotation used to store the source code of the target class
 *
 * @author Shil Sinha
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@GroovyASTTransformationClass('me.shils.internal.transform.SourceCodeASTTransformation' )
@interface SourceCode {
  /**
   * Returns the source code of the annotated class if possible. NOTE: this value is set during compilation. It
   * should NOT be set manually.
   * @return
   */
  String value() default ''
}