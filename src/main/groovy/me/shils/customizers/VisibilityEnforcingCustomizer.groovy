package me.shils.customizers

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.PropertyNode
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.classgen.GeneratorContext
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.codehaus.groovy.control.messages.Message
import org.objectweb.asm.Opcodes

@CompileStatic
class VisibilityEnforcingCustomizer extends CompilationCustomizer {

  VisibilityEnforcingCustomizer() {
    super(CompilePhase.CANONICALIZATION)
  }

  @Override
  void call(final SourceUnit source, final GeneratorContext context, final ClassNode classNode) {
    AccessCheckingVisitor visitor = new AccessCheckingVisitor(source)
    visitor.visitClass(classNode)
  }

  static class AccessCheckingVisitor extends ClassCodeVisitorSupport implements Opcodes {

    static String INFERRED_TYPE_KEY = "${AccessCheckingVisitor.name}_INFERRED_TYPE".toString()
    SourceUnit sourceUnit
    ArrayDeque<ClassNode> classStack = new ArrayDeque<>()

    AccessCheckingVisitor(SourceUnit sourceUnit) {
      this.sourceUnit = sourceUnit
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return sourceUnit
    }

    @Override
    void visitClass(ClassNode classNode) {
      classStack.push(classNode)
      super.visitClass(classNode)
      classStack.pop()
    }

    @Override
    void visitPropertyExpression(PropertyExpression expression) {
      super.visitPropertyExpression(expression)
      ClassNode receiverType = resolveType(expression.objectExpression)

      if ((receiverType instanceof ClassNode && receiverType.isEnum()) || expression.property instanceof GStringExpression)
        return

      PropertyNode propertyNode = receiverType.getProperty(expression.propertyAsString)
      Integer modifiers = propertyNode?.modifiers
      ClassNode ownerType = propertyNode?.field?.owner
      ClassNode inferredType = propertyNode?.field?.type

      if (!propertyNode) {
        MethodNode getterNode = getGetter(receiverType, expression.propertyAsString)
        FieldNode fieldNode = receiverType.getField(expression.propertyAsString)
        if (!fieldNode && !getterNode)
          return

        modifiers = getterNode?.modifiers ?: fieldNode?.modifiers
        ownerType = getterNode?.declaringClass ?: fieldNode?.owner
        inferredType = getterNode?.returnType ?: fieldNode?.type
      }

      if (exceedsAccess(modifiers, ownerType))
        sourceUnit.errorCollector.addErrorAndContinue(Message.create(
                "${classStack.peek().name} exceeds its access to ${ownerType.name}.${expression.propertyAsString}".toString(),
                sourceUnit
        ))
      else
        expression.putNodeMetaData(INFERRED_TYPE_KEY, inferredType)
    }

    @Override
    void visitFieldExpression(FieldExpression expression) {
      super.visitFieldExpression(expression)
      FieldNode fieldNode = expression.field
      if (exceedsAccess(fieldNode.modifiers, fieldNode.owner))
        sourceUnit.errorCollector.addErrorAndContinue(Message.create(
                "${classStack.peek().name} exceeds its access to ${fieldNode.owner.name}.${fieldNode.name}".toString(),
                sourceUnit
        ))
    }

    @Override
    void visitMethodCallExpression(MethodCallExpression expression) {
      super.visitMethodCallExpression(expression)
      if (expression.implicitThis)
        return

      ClassNode receiverType = resolveType(expression.objectExpression)
      MethodNode methodNode = expression.methodTarget ?: resolveMethod(expression, receiverType)
      if (!methodNode)
        return

      ClassNode declaringClass = methodNode.declaringClass
      if (exceedsAccess(methodNode.modifiers, declaringClass))
        sourceUnit.errorCollector.addErrorAndContinue(Message.create(
                "${classStack.peek().name} exceeds its access to ${declaringClass.name}.${methodNode.name}".toString(),
                sourceUnit
        ))
    }

    private boolean exceedsAccess(int modifiers, ClassNode owner) {
      return (modifiers & ACC_PRIVATE && classStack.peek() != owner) ||
              (modifiers & ACC_PROTECTED && !classStack.peek().isDerivedFrom(owner))
    }

    private static MethodNode getGetter(ClassNode classNode, String fieldName) {
      String capitalized = fieldName.capitalize()
      classNode.getGetterMethod('get' + capitalized) ?: classNode.getGetterMethod('is' + capitalized)
    }

    private static ClassNode resolveType(Expression expression) {
      if (expression instanceof VariableExpression) {
        return expression?.accessedVariable?.type ?: expression.type
      } else if (expression instanceof PropertyExpression) {
        return (ClassNode) expression.getNodeMetaData(INFERRED_TYPE_KEY) ?: expression.type
      }
      return expression.type
    }

    private static MethodNode resolveMethod(MethodCallExpression expression, ClassNode receiverType) {
      MethodNode method = expression.methodTarget
      return method ?: receiverType.getMethods(expression.methodAsString).sort { MethodNode it ->
        it.isPublic() ? 0 : 1
      }.find()
    }
  }
}
