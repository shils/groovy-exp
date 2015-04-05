package internal.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import transform.Use

import static org.codehaus.groovy.ast.ClassHelper.getWrapper
import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf
import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.ClassHelper.DYNAMIC_TYPE


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
      if (expr instanceof MethodCallExpression && expr.getMethod() instanceof ConstantExpression) {
        return transformMethodCallExpression((MethodCallExpression) expr)
      } else if (expr instanceof PropertyExpression && expr.getProperty() instanceof ConstantExpression) {
        return transformPropertyExpression((PropertyExpression) expr)
      }
      return expr
    }

    private Expression transformMethodCallExpression(MethodCallExpression mce) {
      List<Expression> categoryMethodCallArgs = generateCategoryMethodCallArgs(mce)
      Expression transformed = generateCategoryMethodCallExpression(mce.methodAsString, categoryMethodCallArgs, mce.type)
      transformed?.setSourcePosition(mce)
      return transformed ?: mce
    }

    private Expression transformPropertyExpression(PropertyExpression pe) {
      List<Expression> categoryMethodCallArgs = [pe.objectExpression]
      Expression transformed = generateCategoryMethodCallExpression('get' + pe.propertyAsString.capitalize(), categoryMethodCallArgs, pe.type)
      transformed?.setSourcePosition(pe)
      return transformed ?: pe
    }

    private Expression generateCategoryMethodCallExpression(String name, List<Expression> categoryMethodCallArgs, ClassNode returnType) {
      MethodNode replacement = findMatchingCategoryMethod(name, categoryMethodCallArgs, returnType)
      if (replacement) {
        Expression smce = callX(category, replacement.name, args(categoryMethodCallArgs))
        return smce
      }
      return null
    }

    private MethodNode findMatchingCategoryMethod(String name, List<Expression> categoryMethodCallArgs, ClassNode returnType) {
      categoryMethods[name]?.find { MethodNode candidate ->
        parametersMatchArgs(candidate.parameters, categoryMethodCallArgs) &&
                implementsInterfaceOrIsSubclassOf(candidate.returnType, returnType)
      }
    }

    private static boolean isCategoryMethod(MethodNode method) {
      return method.isStatic() && method.isPublic() && method.parameters
    }

    private static List<Expression> generateCategoryMethodCallArgs(MethodCallExpression mce) {
      List<Expression> argList = [mce.objectExpression]
      argList.addAll(((TupleExpression) mce.arguments).expressions)
      return argList
    }

    private static boolean parametersMatchArgs(Parameter[] params, List<Expression> argList) {
      for (int i = 0; i < params.length; i++) {
        ClassNode paramType = getWrapper(params[i].type)
        ClassNode argType = getWrapper(argList[i].type)
        if (!implementsInterfaceOrIsSubclassOf(argType, paramType))
          return false
      }
      return true
    }
  }

}
