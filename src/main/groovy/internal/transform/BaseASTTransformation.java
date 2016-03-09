package internal.transform;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;

public abstract class BaseASTTransformation extends AbstractASTTransformation {

  @Override
  public void visit(ASTNode[] nodes, SourceUnit source) {
    init(nodes, source);
    doVisit((AnnotationNode) nodes[0], (AnnotatedNode) nodes[1]);
  }

  public abstract void doVisit(AnnotationNode annotationNode, AnnotatedNode annotatedNode);
}
