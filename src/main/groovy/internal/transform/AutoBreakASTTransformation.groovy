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
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.SwitchStatement
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

  @Override
  void visit(ASTNode[] nodes, SourceUnit source) {
    if (nodes == null || nodes.length != 2 || !(nodes[0] instanceof AnnotationNode) || !(nodes[1] instanceof AnnotatedNode)) {
      throw new GroovyBugError("Internal error: expecting [AnnotationNode, AnnotatedNode] but got: " + (nodes == null ? null : Arrays.asList(nodes)));
    }
    this.sourceUnit = sourceUnit;

    if (MY_TYPE != ((AnnotationNode) nodes[0]).classNode)
      return

    AnnotatedNode target = (AnnotatedNode) nodes[1]
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
    }
  }
}
