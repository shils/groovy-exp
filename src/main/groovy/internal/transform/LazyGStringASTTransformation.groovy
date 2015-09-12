package internal.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.classgen.VariableScopeVisitor
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import transform.LazyGString

import static GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt

/**
 * Handles transformation of code for the {@link LazyGString} annotation.
 *
 * @author Shil Sinha
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class LazyGStringASTTransformation extends AbstractASTTransformation{

  private static Class MY_CLASS = LazyGString.class
  private static ClassNode MY_TYPE = make(MY_CLASS)

  @Override
  void visit(ASTNode[] nodes, SourceUnit source) {
    init(nodes, source)
    AnnotationNode annotationNode = (AnnotationNode) nodes[0]
    AnnotatedNode annotatedNode = (AnnotatedNode) nodes[1]

    if (MY_TYPE != annotationNode.classNode)
      return

    ClassNode target = (ClassNode) annotatedNode
    new GStringExpressionTransformer(source).visitClass(target)
    new VariableScopeVisitor(sourceUnit).visitClass(target)
  }

  static class GStringExpressionTransformer extends ClassCodeExpressionTransformer {

    SourceUnit sourceUnit

    GStringExpressionTransformer(SourceUnit source) {
      sourceUnit = source
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return sourceUnit
    }

    @Override
    Expression transform(Expression expression) {
      expression = super.transform(expression)
      if (!(expression instanceof GStringExpression))
        return expression

      GStringExpression expr = (GStringExpression) expression
      List<ConstantExpression> strings = expr.strings
      List<Expression> values = expr.values.collect { toLazyExpression(it) }
      GStringExpression transformed = new GStringExpression(toVerbatimText(strings, values), strings, values)
      transformed.setSourcePosition(expr)
      transformed.copyNodeMetaData(expr)
      return transformed
    }

    private static Expression toLazyExpression(Expression expr) {
      return new ClosureExpression(null, block(stmt(expr)))
    }

    private static String toVerbatimText(List<ConstantExpression> strings, List<Expression> values) {
      StringBuilder buffer = new StringBuilder()
      Iterator<ConstantExpression> stringIter = strings.iterator()
      Iterator<Expression> valIter = values.iterator()
      buffer.append(stringIter.next().getText())
      while (valIter.hasNext()) {
        buffer.append(valIter.next().getText())
        buffer.append(stringIter.next().getText())
      }
      return buffer.toString()
    }
  }
}
