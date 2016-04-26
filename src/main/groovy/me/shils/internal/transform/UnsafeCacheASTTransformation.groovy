package me.shils.internal.transform

import groovy.transform.CompileStatic
import groovyjarjarasm.asm.Opcodes
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.classgen.ReturnAdder
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import me.shils.transform.UnsafeCache

import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignX
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.fieldX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ifElseS
import static org.codehaus.groovy.ast.tools.GeneralUtils.notNullX
import static org.codehaus.groovy.ast.tools.GeneralUtils.param
import static org.codehaus.groovy.ast.tools.GeneralUtils.params
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX


/**
 * Handles generation of code for the {@link UnsafeCache} annotation.
 *
 * @author Shil Sinha
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class UnsafeCacheASTTransformation extends AbstractASTTransformation implements Opcodes{

  static final ClassNode MY_TYPE = ClassHelper.make(UnsafeCache)
  static final String MY_TYPE_NAME = MY_TYPE.getNameWithoutPackage()

  @Override
  void visit(final ASTNode[] nodes, final SourceUnit source) {
    init(nodes, source)

    AnnotationNode annotation = (AnnotationNode) nodes[0]
    AnnotatedNode target = (AnnotatedNode) nodes[1]

    if (!(target instanceof MethodNode))
      return

    MethodNode method = (MethodNode) target
    ClassNode ownerType = method.declaringClass
    FieldNode cacheField = createCacheField(ownerType, method)
    transformMethod(method, cacheField)

    List<String> resetters = getMemberList(annotation, 'value')
    resetters.each {
      FieldNode field = ownerType.getField(it)
      if (!field)
        return

      String setterName = 'set' + it.capitalize()
      MethodNode setter = ownerType.getSetterMethod(setterName, false) ?: createSetter(ownerType, field, setterName)

      BlockStatement resetBlock = setter.getNodeMetaData(UnsafeCacheMarker.RESET_BLOCK) as BlockStatement
      if (!resetBlock) {
        resetBlock = new BlockStatement()
        setter.code = block(resetBlock, setter.code)
        setter.putNodeMetaData(UnsafeCacheMarker.RESET_BLOCK, resetBlock)
      }
      resetBlock.addStatement(setNullStatement(cacheField))
    }
  }

  private void transformMethod(MethodNode method, FieldNode cacheField) {
    new ReturnAdder().visitMethod(method)
    new ReturnCachingVisitor(source: sourceUnit, cacheField: cacheField).visitMethod(method)
    BlockStatement newBody = block(
            ifElseS(
                    notNullX(fieldX(cacheField)),
                    returnS(fieldX(cacheField)),
                    method.code
            )
    )
    method.code = newBody
  }

  private static Statement setNullStatement(FieldNode cacheField) {
    assignS(fieldX(cacheField), constX(null))
  }

  private static FieldNode createCacheField(ClassNode owner, MethodNode method) {
    FieldNode field = new FieldNode('cached' + method.name.capitalize(), ACC_PRIVATE, method.returnType, owner, null)
    owner.addField(field)
    field
  }

  private static MethodNode createSetter(ClassNode owner, FieldNode field, String setterName) {
    Parameter param = param(field.type, field.name)
    BlockStatement code = block(assignS(fieldX(field), varX(param)))
    MethodNode setter = new MethodNode(setterName, ACC_PUBLIC, ClassHelper.VOID_TYPE, params(param), ClassNode.EMPTY_ARRAY, code)
    owner.addMethod(setter)
    setter
  }

  private enum UnsafeCacheMarker {
    RESET_BLOCK
  }

  static class ReturnCachingVisitor extends ClassCodeVisitorSupport {

    SourceUnit source
    FieldNode cacheField

    @Override
    protected SourceUnit getSourceUnit() {
      return source
    }

    @Override
    void visitReturnStatement(ReturnStatement statement) {
      statement.expression = assignX(fieldX(cacheField), statement.expression)
    }
  }

}