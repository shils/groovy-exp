package transform

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @author Shil Sinha
 */
@Retention(RetentionPolicy.SOURCE)
@Target([ElementType.METHOD, ElementType.TYPE])
@GroovyASTTransformationClass('internal.transform.UseASTTransformation')
@interface Use {

  Class value()

  boolean override() default false
}
