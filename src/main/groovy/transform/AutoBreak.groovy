package transform

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Class and Method level annotation used to add a break statement to the end of the body of
 * every non-empty (unless {@link AutoBreak#includeEmptyCases()} is true) case statement within a switch.
 *
 * @author Shil Sinha
 */
@Retention(RetentionPolicy.CLASS)
@Target([ElementType.TYPE, ElementType.METHOD])
@GroovyASTTransformationClass('internal.transform.AutoBreakASTTransformation')
@interface AutoBreak {

  boolean includeEmptyCases() default false
}
