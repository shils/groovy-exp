package internal.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import transform.Use
import org.codehaus.groovy.ast.ClassHelper

import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX

import static org.codehaus.groovy.ast.ClassHelper.make


@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class UseASTTransformation extends AbstractASTTransformation {

  static final Class MY_CLASS = Use.class
  static final ClassNode MY_TYPE = make(MY_CLASS)

  @Override
  void visit(ASTNode[] nodes, SourceUnit source) {
    init(nodes, source)
    AnnotationNode annotationNode = (AnnotationNode) nodes[0]
    AnnotatedNode annotatedNode = (AnnotatedNode) nodes[1]

    if (MY_TYPE != annotationNode.classNode)
      return

    if (!(annotatedNode instanceof MethodNode))
      return

    MethodNode method = (MethodNode) annotatedNode
    ClassNode category = getMemberClassValue(annotationNode, 'value')
    CategoryMethodCallExpressionTransformer transformer = new CategoryMethodCallExpressionTransformer(sourceUnit, category)
    transformer.init()
    if (transformer.categoryMethods)
      transformer.visitMethod(method)
  }

  private static class CategoryMethodCallExpressionTransformer extends ClassCodeExpressionTransformer {

    private final SourceUnit source
    private final ClassNode category
    private final Map<String, List<MethodNode>> categoryMethods

    CategoryMethodCallExpressionTransformer(SourceUnit source, ClassNode category) {
      this.source = source
      this.category = category
      categoryMethods = new HashMap<String, List<MethodNode>>()
    }

    void init() {
      for (MethodNode method: category.getMethods()) {
        if (isCategoryMethod(method))
          categoryMethods.get(method.name, []) << method
      }
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return source
    }

    @Override
    Expression transform(Expression expression) {
      Expression expr = super.transform(expression)
      if (isEligibleForReplacement(expr)) {
        MethodCallExpression mce = (MethodCallExpression) expr
        List<Expression> categoryMethodCallArgs = generateCategoryMethodCallArgs(mce)
        MethodNode replacement = findMatchingCategoryMethod(mce.methodAsString, categoryMethodCallArgs)
        if (replacement) {
          Expression smce = callX(category, replacement.name, args(categoryMethodCallArgs))
          smce.setSourcePosition(mce)
          return smce
        }
      }
      return expr
    }

    private MethodNode findMatchingCategoryMethod(String name, List<Expression> categoryMethodCallArgs) {
      categoryMethods[name]?.find { MethodNode candidate ->
        if (parametersMatchArgs(candidate.parameters, categoryMethodCallArgs))
          return candidate
      }
    }

    private static boolean isCategoryMethod(MethodNode method) {
      return method.isStatic() && method.isPublic() && method.parameters && !method.parameters[0].dynamicTyped
    }

    private static boolean isEligibleForReplacement(Expression expr) {
      return expr instanceof MethodCallExpression && expr.method instanceof ConstantExpression
    }

    private static List<Expression> generateCategoryMethodCallArgs(MethodCallExpression mce) {
      List<Expression> argXList = [mce.objectExpression]
      argXList.addAll(((TupleExpression) mce.arguments).expressions)
      return argXList
    }

    private static boolean parametersMatchArgs(Parameter[] params, List<Expression> argXList) {
      for (int i = 0; i < params.length; i++) {
        if (argXList[i].type != params[i].type)
          return false
      }
      return true
    }
  }

}
