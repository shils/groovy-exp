package me.shils.util.logging

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.transform.LogASTTransformation

import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.fieldX

/**
 * Convenience trait that can be used with existing {@link LogASTTransformation.LoggingStrategy}
 * implementations to add support for logging with enum constructors.
 *
 * @see me.shils.util.logging.Log.JavaUtilLoggingStrategy
 */
@CompileStatic
trait EnumConstructorLoggingStrategy implements LogASTTransformation.LoggingStrategy {

  @Override
  FieldNode addLoggerFieldToClass(ClassNode classNode, String fieldName, String categoryName) {
    FieldNode fn = super.addLoggerFieldToClass(classNode, fieldName, categoryName)
    if (classNode.isEnum()) {
      Expression initialValue = fn.initialValueExpression
      fn.initialValueExpression = null
      MethodNode clInit = classNode.getMethod('<clinit>', Parameter.EMPTY_ARRAY)
      clInit.code = block(assignS(fieldX(fn), initialValue), clInit.code)
    }
    fn
  }
}