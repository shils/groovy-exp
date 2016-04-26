package me.shils.customizers

import org.codehaus.groovy.control.CompilerConfiguration

class VisibilityEnforcingCustomizerTest extends GroovyShellTestCase {

  @Override
  GroovyShell createNewShell() {
    def config = new CompilerConfiguration()
    config.addCompilationCustomizers(new VisibilityEnforcingCustomizer())
    new GroovyShell(config)
  }

  void testPrivateMethodsShouldNotBeAccessible(){
    shouldFail '''
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

  void testPublicMethodsShouldBeAccessible(){
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

  void testPrivateFieldsShouldNotBeAccessible(){
    shouldFail '''
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

  void testPublicFieldsShouldBeAccessible(){
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

  @Override
  String shouldFail(String script) {
    shouldFail {
      shell.evaluate(script)
    }
  }

}
