package me.shils.internal.transform

import me.shils.transform.SourceCode
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer

class SourceCodeASTTransformationTest extends GroovyShellTestCase {
  @Override
  protected GroovyShell createNewShell() {
    def icz = new ImportCustomizer().addImports('me.shils.transform.SourceCode')
    def config = new CompilerConfiguration().addCompilationCustomizers(icz)
    new GroovyShell(config)
  }

  void testOnlyAnnotatedClassesHaveSourceCodeValue() {
    Map<String, Class> results = shell.evaluate '''
      @SourceCode
      class Foo {}     
      class Bar {}
      @SourceCode 
      class Baz {}      
      [Foo: Foo, Bar: Bar, Baz: Baz]
    '''
    ['Foo', 'Baz'].each {
      assert ((Class) results[it]).getAnnotation(SourceCode).value().contains("class $it {")
    }
    assert !((Class) results['Bar']).getAnnotation(SourceCode)
  }

  void testAllClassesHaveSourceCodeValueIfAppliedAsCustomizer() {
    def config = new CompilerConfiguration().addCompilationCustomizers(new ASTTransformationCustomizer(SourceCode))
    shell = new GroovyShell(config)
    Map<String, Class> results = shell.evaluate '''
      class Foo {}
      class Bar {}
      class Baz {}
      [Foo: Foo, Bar: Bar, Baz: Baz]
    '''
    results.values().each {
      assert it.getAnnotation(SourceCode).value().contains("class ${it.getSimpleName()} {".toString())
    }
  }
}
