package me.shils.internal.transform

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer


class AutoBreakASTTransformationTest extends GroovyShellTestCase {

  @Override
  protected GroovyShell createNewShell() {
    def icz = new ImportCustomizer().addImports 'me.shils.transform.AutoBreak'
    def config = new CompilerConfiguration().addCompilationCustomizers(icz)
    new GroovyShell(config)
  }

  @Override
  protected void assertScript(String script) throws Exception {
    shell.evaluate(script)
  }

  void testNoFallThroughAfterNonEmptyCaseStatement() {
    assertScript '''
      @AutoBreak
      int test() {
        int result = 0
        switch(1) {
          case 1:
            result = 1
          case 2:
            result = 2
          default:
            result = 3
        }
        return result
      }
      assert test() == 1
    '''
  }

  void testFallThroughAfterEmptyCaseStatementByDefault() {
    assertScript '''
      @AutoBreak
      int test() {
        int result = 0
        switch(1) {
          case 1:
          case 2:
            result = 2
          default:
            result = 3
        }
        return result
      }
      assert test() == 2
    '''
  }

  void testBreakAfterEmptyCaseStatementWhenSpecified() {
    assertScript '''
      @AutoBreak(includeEmptyCases = true)
      int test() {
        int result = 0
        switch(1) {
          case 1:
          case 2:
            result = 2
          default:
            result = 3
        }
        return result
      }
      assert test() == 0
    '''
  }

  void testAutoBreakOnClass() {
    assertScript '''
      @AutoBreak
      class Foo {
        int test() {
          int result = 0
          switch(1) {
            case 1:
              result = 1
            case 2:
              result = 2
            default:
              result = 3
          }
          return result
        }
      }
      assert new Foo().test() == 1
    '''
  }

  void testMethodAutoBreakOverridesClassAutoBreak() {
    assertScript '''
      @AutoBreak(includeEmptyCases = true)
      class Foo {

        @AutoBreak
        int test() {
          int result = 0
          switch(1) {
            case 1:
            case 2:
              result = 2
            default:
              result = 3
          }
          return result
        }
      }
      assert new Foo().test() == 2
    '''

    assertScript '''
      @AutoBreak
      class Foo {

        @AutoBreak(includeEmptyCases = true)
        int test() {
          int result = 0
          switch(1) {
            case 1:
            case 2:
              result = 2
            default:
              result = 3
          }
          return result
        }
      }
      assert new Foo().test() == 0
    '''
  }

  void testNestedClassAutoBreakOverridesOuterAutoBreak() {
    assertScript '''
      @AutoBreak(includeEmptyCases = true)
      class Foo {

        @AutoBreak
        static class Bar {
          int test() {
            int result = 0
            switch(1) {
              case 1:
              case 2:
                result = 2
              default:
                result = 3
            }
            return result
          }
        }
      }
      assert new Foo.Bar().test() == 2
    '''

    assertScript '''
      @AutoBreak
      class Foo {

        @AutoBreak(includeEmptyCases = true)
        static class Bar {
          int test() {
            int result = 0
            switch(1) {
              case 1:
              case 2:
                result = 2
              default:
                result = 3
            }
            return result
          }
        }
      }
      assert new Foo.Bar().test() == 0
    '''
  }

  void testTripleNestedAutoBreak() {
    assertScript '''
      @AutoBreak
      class Foo {

        @AutoBreak(includeEmptyCases = true)
        static class Bar {

          @AutoBreak
          int test() {
            int result = 0
            switch(1) {
              case 1:
              case 2:
                result = 2
              default:
                result = 3
            }
            return result
          }
        }
      }
      assert new Foo.Bar().test() == 2
    '''

    assertScript '''
      @AutoBreak(includeEmptyCases = true)
      class Foo {

        @AutoBreak
        static class Bar {

          @AutoBreak(includeEmptyCases = true)
          int test() {
            int result = 0
            switch(1) {
              case 1:
              case 2:
                result = 2
              default:
                result = 3
            }
            return result
          }
        }
      }
      assert new Foo.Bar().test() == 0
    '''
  }
}