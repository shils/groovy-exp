package internal.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.GroovyBugError
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import transform.AutoBreak

/**
 * Handles generation of code for the {@link AutoBreak} annotation.
 *
 * @author Shil Sinha
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class AutoBreakASTTransformation extends ClassCodeVisitorSupport implements ASTTransformation {

  static final Class MY_CLASS = AutoBreak.class
  static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS)

  SourceUnit sourceUnit
  private boolean includeEmptyCases

  @Override
  void visit(ASTNode[] nodes, SourceUnit source) {
    if (nodes == null || nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
      throw new GroovyBugError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + (nodes == null ? null : Arrays.asList(nodes)));
    }
    this.sourceUnit = sourceUnit;

    AnnotationNode annotation = (AnnotationNode) nodes[0]
    AnnotatedNode target = (AnnotatedNode) nodes[1]

    if (MY_TYPE != annotation.classNode)
      return

    includeEmptyCases = getMemberBooleanValue(annotation, 'includeEmptyCases')
    if (target instanceof ClassNode) {
      ((ClassNode) target).visitContents(this)
    } else if (target instanceof MethodNode) {
      visitClassCodeContainer(((MethodNode) target).code)
    }
  }

  @Override
  void visitCaseStatement(CaseStatement statement) {
    super.visitCaseStatement(statement)
    if (statement.code instanceof BlockStatement) {
      ((BlockStatement) statement.code).addStatement(new BreakStatement())
    } else if (includeEmptyCases && statement.code instanceof EmptyStatement) {
      statement.code = GeneralUtils.block(new BreakStatement())
    }
  }

  private static boolean getMemberBooleanValue(AnnotationNode annotation, String name) {
    Expression member = annotation.getMember(name)
    if (member instanceof ConstantExpression) {
      return ((ConstantExpression) member).isTrueExpression()
    }
    return false
  }
}
