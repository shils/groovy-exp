package me.shils.internal.transform

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.CodeVisitorSupport
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.expr.AttributeExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.FieldExpression
import org.codehaus.groovy.ast.expr.PostfixExpression
import org.codehaus.groovy.ast.expr.PrefixExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.GroovyASTTransformation
import me.shils.transform.LocalStatic

import java.lang.reflect.Modifier

/**
 * Handles generation of code for the {@link LocalStatic} annotation.
 *
 * @author Shil Sinha
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
@CompileStatic
class LocalStaticASTTransformation extends BaseASTTransformation {

  static final Class MY_CLASS = LocalStatic.class
  static final ClassNode MY_TYPE = ClassHelper.make(MY_CLASS)
  static final String MY_TYPE_NAME = '@' + MY_TYPE.getNameWithoutPackage()

  @Override
  void doVisit(final AnnotationNode annotationNode, final AnnotatedNode annotatedNode) {
    if (MY_TYPE != annotationNode.classNode) {
      return
    }
    if (!(annotatedNode instanceof DeclarationExpression)) {
      return
    }

    DeclarationExpression declaration = (DeclarationExpression) annotatedNode
    if (isValidDeclaration(declaration)) {
      Set<FieldNode> generatedConstantFields = (Set<FieldNode>) declaration.declaringClass.getNodeMetaData(LocalStaticASTTransformation.class) ?:
              new HashSet<FieldNode>()
      FieldNode field = new FieldNode(
              'LOCALSTATIC$' + (generatedConstantFields.size()),
              ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
              declaration.variableExpression.originType,
              declaration.declaringClass,
              declaration.rightExpression
      )
      declaration.declaringClass.addField(field)
      declaration.rightExpression = new FieldExpression(field)
      generatedConstantFields.add(field)
      declaration.declaringClass.putNodeMetaData(LocalStaticASTTransformation, generatedConstantFields)
    }
  }

  private boolean isValidDeclaration(DeclarationExpression declaration) {
    if (declaration.rightExpression instanceof EmptyExpression) {
      addError("$MY_TYPE_NAME declarations must include an initialization expression", declaration)
    } else if (declaration.isMultipleAssignmentDeclaration()) {
      addError("Multiple assignment is not permitted in $MY_TYPE_NAME declarations", declaration)
    } else if (!Modifier.isFinal(declaration.variableExpression.modifiers)) {
      addError("$MY_TYPE_NAME declarations must be final", declaration)
    } else {
      Map<Expression, String> nonStaticExpressions = [:]
      declaration.rightExpression.visit(new NonStaticIdentifyingVisitor(nonStaticExpressions))
      return !nonStaticExpressions.each {
        addError(it.value, it.key)
      }
    }
    return false
  }

  private static class NonStaticIdentifyingVisitor extends CodeVisitorSupport {

    Map<Expression, String> nonStaticExpressions

    NonStaticIdentifyingVisitor(Map<Expression, String> nonStaticExpressions) {
      this.nonStaticExpressions = nonStaticExpressions
    }

    @Override
    void visitVariableExpression(VariableExpression expression) {
      if (expression.isThisExpression()) {
        addError(expression, "References to 'this' object are not permitted in $MY_TYPE_NAME declarations")
      } else if(expression.isSuperExpression()) {
        addError(expression, "References to super are not permitted in $MY_TYPE_NAME declarations")
      } else if (!(expression.accessedVariable instanceof FieldNode) || !isFieldValid((FieldNode) expression.accessedVariable)) {
        addError(expression, "Variables used in $MY_TYPE_NAME declarations must refer to static final fields")
      }
    }

    @Override
    void visitPropertyExpression(PropertyExpression expression) {
      if (expression.objectExpression instanceof ClassExpression) {
        ClassNode owner = expression.objectExpression.type
        FieldNode field = owner.getDeclaredField(expression.propertyAsString)
        if (field.isStatic() && field.isFinal()) {
          return
        }
      }
      addError(expression, "Properties referenced in $MY_TYPE_NAME declarations must refer to static final fields")
    }

    @Override
    void visitPostfixExpression(PostfixExpression expression) {
      addError(expression, "Postfix expressions are not permitted in $MY_TYPE_NAME declarations")
    }

    @Override
    void visitPrefixExpression(PrefixExpression expression) {
      addError(expression, "Prefix expressions are not permitted in $MY_TYPE_NAME declarations")
    }

    @Override
    void visitAttributeExpression(AttributeExpression expression) {
      addError(expression, "Attribute expressions are not permitted in $MY_TYPE_NAME declarations")
    }

    @Override
    void visitFieldExpression(FieldExpression expression) {
      if (!isFieldValid(expression.field)) {
        addError(expression, "Fields referenced in $MY_TYPE_NAME declarations must be static and final")
      }
    }

    void addError(Expression expression, String message) {
      nonStaticExpressions << [(expression): message]
    }

    private static boolean isFieldValid(FieldNode field) {
      field.isStatic() && field.isFinal()
    }
  }
}