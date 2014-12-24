package transform

import groovy.transform.CompileStatic
import exception.SnaggedException
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.CatchStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.ThrowStatement
import org.codehaus.groovy.ast.stmt.TryCatchStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation

import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

/**
 * Handles generation of code for the {@link SnagArgs} annotation.
 *
 * @author Shil Sinha
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class SnagArgsASTTransformation extends AbstractASTTransformation {

  static final Class<SnagArgs> MY_CLASS = SnagArgs.class
  static final ClassNode MY_TYPE = make(MY_CLASS)
  static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage()
  static final ClassNode EXCEPTION_TYPE = make(Exception.class)
  static final ClassNode DEFAULT_EXCEPTION_TYPE = EXCEPTION_TYPE
  static final ClassNode SNAGGED_EXCEPTION_TYPE = make(SnaggedException.class)
  static final ClassNode MAP_TYPE = make(HashMap.class)


  @Override
  void visit(final ASTNode[] nodes, final SourceUnit source) {
    init(nodes, source)
    AnnotationNode annotationNode = (AnnotationNode) nodes[0]
    AnnotatedNode annotatedNode = (AnnotatedNode) nodes[1]
    if (MY_TYPE != annotationNode.classNode || !(annotatedNode instanceof MethodNode))
      return

    ClassNode exceptionType = getMemberClassValue(annotationNode, "exception", DEFAULT_EXCEPTION_TYPE)
    if (!exceptionType.isDerivedFrom(EXCEPTION_TYPE)) {
      addError("{$exceptionType.name} is not derived from ${EXCEPTION_TYPE.name}", annotationNode)
      return
    }

    MethodNode methodNode = (MethodNode) annotatedNode
    if (methodNode.isAbstract()) {
      addError("Annotation " + MY_TYPE_NAME + " cannot be used for abstract methods", methodNode)
      return
    }

    if (methodNode.parameters.length == 0) {
      addError("Annotation " + MY_TYPE_NAME + " cannot be used for parameter-less methods", methodNode)
      return
    }

    List<String> paramsToSnag = (getMemberList(annotationNode, "params") ?: methodNode.parameters.collect { Parameter it -> it.name }) as List<String>
    List<String> invalidParams = validateParams(methodNode, paramsToSnag)
    if (!invalidParams.isEmpty()) {
      addError("$invalidParams are not parameter names for this method", methodNode)
      return
    }

    methodNode.setCode(calculateSnagArgsStatements(methodNode, paramsToSnag, exceptionType))
  }

  private Statement calculateSnagArgsStatements(MethodNode methodNode, List<String> paramsToSnag, ClassNode exceptionType) {
    BlockStatement newBody = new BlockStatement()
    paramsToSnag.each { String paramName ->
      newBody.addStatement(snagS(paramName))
    }

    Statement finallyStatement = EmptyStatement.INSTANCE
    Statement tryCatchBlock = new TryCatchStatement(methodNode.getCode(), finallyStatement)
    tryCatchBlock.addCatch(catchS(paramsToSnag, exceptionType))
    newBody.addStatement(tryCatchBlock)
    return newBody
  }

  private static Statement snagS(String paramName) {
      Expression argCopyX = varX('_' + paramName)
      return declS(argCopyX, varX(paramName))
  }

  private static CatchStatement catchS(List<String> paramsToSnag, ClassNode exceptionType) {
    BlockStatement catchBlock = new BlockStatement()
    Parameter catchParameter = new Parameter(exceptionType, "_ex")
    Expression argsMapX = varX("_args")
    catchBlock.addStatement(declS(argsMapX, ctorX(MAP_TYPE)))
    paramsToSnag.each { String paramName ->
      catchBlock.addStatement(putS(argsMapX, paramName))
    }
    catchBlock.addStatement(new ThrowStatement(ctorX(SNAGGED_EXCEPTION_TYPE, args(argsMapX, varX(catchParameter)))))
    return new CatchStatement(catchParameter, catchBlock)
  }

  private static List<String> validateParams(MethodNode methodNode, List<String> paramsToSnag) {
    return paramsToSnag.findAll { String paramName ->
      methodNode.parameters.every { Parameter it -> it.name != paramName }
    } as List<String>
  }

  private static Statement putS(Expression result, String paramName) {
    MethodCallExpression put = callX(result, "put", args(constX(paramName), varX("_" + paramName)))
    put.setImplicitThis(false)
    return stmt(put)
  }
}


