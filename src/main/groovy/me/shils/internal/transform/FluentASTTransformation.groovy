package me.shils.internal.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import me.shils.transform.Fluent
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GeneralUtils.params
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

/**
 * Handles generation of code for the {@link Fluent} annotation.
 *
 * @author Shil Sinha
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class FluentASTTransformation extends AbstractASTTransformation {

  static final Class MY_CLASS = Fluent
  static final ClassNode MY_TYPE = make(MY_CLASS)
  static final String MY_TYPE_NAME = '@' + MY_TYPE.getNameWithoutPackage()

  @Override
  void visit(ASTNode[] nodes, SourceUnit source) {
    init(nodes, source)
    AnnotationNode anno = (AnnotationNode) nodes[0]
    AnnotatedNode target = (AnnotatedNode) nodes[1]

    if (MY_TYPE != anno.classNode)
      return
    if (!(target instanceof ClassNode))
      return

    ClassNode cNode = (ClassNode) target
    for (PropertyNode pNode: cNode.getProperties()) {
      MethodNode setter = cNode.getSetterMethod('set' + pNode.name.capitalize(), false)
      if (!setter && !pNode.isPrivate() && !pNode.isStatic()) {
        addSetter(cNode, pNode.field)
      }
    }
  }

  void addSetter(ClassNode cNode, FieldNode fNode) {
    BlockStatement body = new BlockStatement()
    String name = fNode.name
    body.addStatement(assignS(propX(varX('this'),  name), varX(name)))
    body.addStatement(returnS(varX('this')))
    cNode.addSyntheticMethod('set' + name.capitalize(), ACC_PUBLIC, cNode.getPlainNodeReference(),
            params(param(fNode.type, name)), ClassNode.EMPTY_ARRAY, body)
  }

}
