package me.shils.internal.transform

import groovy.transform.CompileStatic
import me.shils.transform.SourceCode
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

/**
 * Handles generation of code for the {@link me.shils.transform.SourceCode} annotation.
 *
 * @author Shil Sinha
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class SourceCodeASTTransformation extends AbstractASTTransformation {
  static final Class MY_CLASS = SourceCode.class
  static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS)
  static final MY_TYPE_NAME = '@' + MY_TYPE.getNameWithoutPackage()

  void visit(ASTNode[] nodes, SourceUnit source) {
    init(nodes, source)
    AnnotationNode annotationNode = (AnnotationNode) nodes[0]
    AnnotatedNode annotatedNode = (AnnotatedNode) nodes[1]

    //workaround for GROOVY-8402
    if (annotatedNode.putNodeMetaData(SourceCodeASTTransformation, true)) {
      return
    }

    if (MY_TYPE != annotationNode.classNode) {
      return
    }
    if (!(annotatedNode instanceof ClassNode)) {
      return
    }

    String sourceCode = getMemberStringValue(annotationNode, 'value')
    boolean appliedByCustomizer = !annotatedNode.getAnnotations(MY_TYPE)
    if (!appliedByCustomizer && sourceCode) {
      addError("$MY_TYPE_NAME#value must not be set prior to compilation.", annotationNode)
      return
    }

    if (!sourceCode) {
      sourceCode = source.source.reader.text
      if (appliedByCustomizer) {
        //cache value in dummy annotation for other classes in source unit
        annotationNode.setMember('value', GeneralUtils.constX(sourceCode))
      }
    }

    //the annotation that can be used to lookup source code at runtime
    AnnotationNode holder
    if (appliedByCustomizer) {
      holder = new AnnotationNode(MY_TYPE)
      annotatedNode.addAnnotation(holder)
    } else {
      holder = annotationNode
    }

    holder.setMember('value', GeneralUtils.constX(sourceCode))
  }
}
