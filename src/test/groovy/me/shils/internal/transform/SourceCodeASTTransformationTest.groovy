package me.shils.internal.transform

import me.shils.transform.SourceCode
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer

import java.nio.file.Files
import java.nio.file.Path

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

  void testDifferentClassesCompiledUsingSameCustomizer() {
    def config = new CompilerConfiguration().addCompilationCustomizers(new ASTTransformationCustomizer(SourceCode))
    def gcl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), config)
    Class Foo = gcl.parseClass('class Foo {}')
    Class Bar = gcl.parseClass('class Bar {}')
    assert Foo.getAnnotation(SourceCode).value() == 'class Foo {}'
    assert Bar.getAnnotation(SourceCode).value() == 'class Bar {}'
  }

  void testGSECompatibility() {
    Path tmpDir = null
    File script = null
    try {
      tmpDir = Files.createTempDirectory('tmp' + getClass().simpleName)
      script = new File(tmpDir.toFile(), 'Pls')
      script << '''
        class Foo {}
        [Foo: Foo, Pls: getClass()]
      '''
      def gse = new GroovyScriptEngine([tmpDir.toUri().toURL()] as URL[])
      gse.config = new CompilerConfiguration().addCompilationCustomizers(new ASTTransformationCustomizer(SourceCode))
      Map<String, Class> results = gse.run('Pls', new Binding())
      results.values().each {
        assert it.getAnnotation(SourceCode).value() == script.text
      }
    } finally {
      tmpDir?.deleteDir()
      script?.delete()
    }
  }
}
