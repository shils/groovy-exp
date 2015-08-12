package customizers

import org.codehaus.groovy.control.CompilerConfiguration

class VisibilityEnforcingCustomizerTest extends GroovyTestCase {
  CompilerConfiguration configuration
  VisibilityEnforcingCustomizer customizer

  void setUp() {
    configuration = new CompilerConfiguration()
    customizer = new VisibilityEnforcingCustomizer()
    configuration.addCompilationCustomizers(customizer)
  }

  void testPrivateMethodsShouldNotBeAcessible(){
    def shell = new GroovyShell(configuration)
    shouldFail {
      shell.evaluate '''
        class A {
          private void foo() {
            print "I'm private"
          }
        }

        class B {
          void doAFoo(A a) {
            a.foo()
          }
        }
        null
      '''
    }
  }

  void testPublicMethodsShouldBeAcessible(){
    def shell = new GroovyShell(configuration)
    shell.evaluate '''
        class A {
          public void foo() {
            print "I'm public"
          }
        }

        class B {
          void doAFoo(A a) {
            a.foo()
          }
        }
        null
    '''
  }

  void testPrivateFieldsShouldNotBeAcessible(){
    def shell = new GroovyShell(configuration)
    shouldFail {
      shell.evaluate '''
        class A {
          private int x
        }

        class B {
          void printAX(A a) {
            println a.x
          }
        }
        null
      '''
    }
  }

  void testPublicFieldsShouldBeAcessible(){
    def shell = new GroovyShell(configuration)
    shell.evaluate '''
        class A {
          public int x
        }

        class B {
          void printAX(A a) {
            println a.x
          }
        }
        null
    '''
  }

}
