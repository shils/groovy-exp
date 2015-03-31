package transform;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Method annotation used to catch exceptions and throw a new {@link exception.SnaggedException},
 * passing in the caught exception and a map of the method arguments for the exception throwing invocation.
 * <p/>
 * The {@code @SnagArgs} annotation instructs the compiler to execute the AST transformation
 * in {@link internal.transform.SnagArgsASTTransformation}, which places the body of the annotated
 * method in a try block and adds a catch block as described above.
 *
 * @author Shil Sinha
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@GroovyASTTransformationClass("internal.transform.SnagArgsASTTransformation")
public @interface SnagArgs{

  /**
   * List of method parameters to include in the map of parameter names to argument values passed to
   * the {@link exception.SnaggedException} instance constructed in the case of a caught exception.
   */
  String[] params() default {};

  /**
   * Type or supertype of exception that will prompt snagging of method arguments when thrown.
   */
  Class exception() default Exception.class;

  /**
   * Whether to capture the arguments after catching the exception or before executing the method (default)
   */
  boolean captureAtException() default false;

}
