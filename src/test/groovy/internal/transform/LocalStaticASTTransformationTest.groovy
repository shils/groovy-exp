package internal.transform

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer


class LocalStaticASTTransformationTest extends GroovyShellTestCase {

  @Override
  protected GroovyShell createNewShell() {
    def icz = new ImportCustomizer()
            .addImports('transform.LocalStatic', 'groovy.transform.ASTTest')
            .addStaticStars('org.codehaus.groovy.control.CompilePhase')
    def config = new CompilerConfiguration().addCompilationCustomizers(icz)
    new GroovyShell(config)
  }

  void testAnnotatedVariableInitializedToClassField() {
    shell.evaluate '''
      class Foo {
        List<String> test() {
          @LocalStatic final List<String> x = ['abc', 'def']
          x
        }
      }
      assert new Foo().test().is(Foo.LOCALSTATIC$0)
      null
    '''
  }

  void testEmptyDeclarationShouldFail() {
    def message = shouldFail '''
      class Foo {
        List<String> test() {
          @LocalStatic final List<String> x
          null
        }
      }
      null
    '''
    assert message.contains('must include an initialization expression')
  }

  void testNonFinalDeclarationShouldFail() {
    def message = shouldFail '''
      class Foo {
        List<String> test() {
          @LocalStatic List<String> x = ['a', 'b']
          x
        }
      }
      null
    '''
    assert message.contains('must be final')
  }

  void testMultipleAssignmentShouldFail() {
    def message = shouldFail '''
      class Foo {
        List<String> test() {
          @LocalStatic final def (a, b) = ['a', 'b']
          [a, b]
        }
      }
      null
    '''
    assert message.contains('Multiple assignment is not permitted')
  }

  void testExpressionReferencingLocalVariableShouldFail() {
    def message = shouldFail '''
      class Foo {
        List<String> test() {
          String local = 'abc'
          @LocalStatic final List<String> x = [local, 'def']
          x
        }
      }
      null
    '''
    assert message.contains('must refer to static final fields')
  }

  void testExpressionReferencingParameterShouldFail() {
    def message = shouldFail '''
      class Foo {
        List<String> test(String param) {
          @LocalStatic final List<String> x = [param, 'def']
          x
        }
      }
      null
    '''
    assert message.contains('must refer to static final fields')
  }

  void testExpressionReferencingThisObjectShouldFail() {
    def message = shouldFail '''
      class Foo {
        List<String> test() {
          @LocalStatic final List<String> x = [bar(), 'def']
          x
        }

        String bar() { 'abc' }
      }
      null
    '''
    assert message.contains("References to 'this' object are not permitted")
  }

  void testExpressionReferencingSuperShouldFail() {
    def message = shouldFail '''
      class Baz {
        List<String> test() { ['abc'] }
      }

      class Foo extends Baz {
        @Override
        List<String> test() {
          @LocalStatic final List<String> x = super.test()
          x
        }
      }
      null
    '''
    assert message.contains("References to super are not permitted")
  }

  void testPostFixExpressionShouldFail() {
    def message = shouldFail '''
      class Foo {
        static final int y = 1

        List<Integer> test() {
          @LocalStatic final List<Integer> x = [3, y++]
          x
        }
      }
      null
    '''
    assert message.contains("Postfix expressions are not permitted")
  }

  void testPreFixExpressionShouldFail() {
    def message = shouldFail '''
      class Foo {
        static final int y = 1

        List<Integer> test() {
          @LocalStatic final List<Integer> x = [3, ++y]
          x
        }
      }
      null
    '''
    assert message.contains("Prefix expressions are not permitted")
  }

  void testPropertyExpressionShouldFail() {
    def message = shouldFail '''
      class Foo {
        String y

        List<String> test() {
          @LocalStatic final List<String> x = ['abc', this.y]
          x
        }
      }
      null
    '''
    assert message.contains("must refer to static final fields")
  }

  void testAttributeExpressionShouldFail() {
    def message = shouldFail '''
      class Foo {
        String y

        List<String> test() {
          @LocalStatic final List<String> x = ['abc', this.@y]
          x
        }
      }
      null
    '''
    assert message.contains("Attribute expressions are not permitted")
  }

  void testExpressionReferencingStaticFinalFieldOfDeclaringClass() {
    shell.evaluate '''
      class Foo {
        static final String CONSTANT = 'def'

        List<String> test() {
          @LocalStatic final List<String> x = ['abc', CONSTANT]
          x
        }
      }
      def foo = new Foo()
      assert foo.test() == ['abc', 'def']
      assert foo.test().is(Foo.LOCALSTATIC$0)
    '''
  }

  void testExpressionReferencingStaticFinalFieldOfExternalClass() {
    shell.evaluate '''
      class Bar {
        static final String CONSTANT = 'def'
      }

      class Foo {
        List<String> test() {
          @LocalStatic final List<String> x = ['abc', Bar.CONSTANT]
          x
        }
      }
      def foo = new Foo()
      assert foo.test() == ['abc', 'def']
      assert foo.test().is(Foo.LOCALSTATIC$0)
    '''
  }

  @Override
  String shouldFail(String script) {
    shouldFail {
      shell.evaluate(script)
    }
  }
}
