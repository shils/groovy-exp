package me.shils.internal.transform


class LazyGStringASTTransformationTest extends GroovyTestCase {

  void testLazyEvaluation() {
    assertScript '''
      import me.shils.transform.LazyGString
      import groovy.transform.CompileStatic

      @LazyGString
      @CompileStatic
      class A {
        void test(){
          int foo = 0
          int bar = 0
          GString g = "$foo and ${bar + 2}"
          assert g == '0 and 2'
          foo = 1
          bar = 1
          assert g == '1 and 3'
        }
      }
      new A().test()
    '''
  }
}


