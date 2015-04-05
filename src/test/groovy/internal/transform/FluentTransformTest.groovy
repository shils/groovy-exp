package internal.transform


class FluentTransformTest extends GroovyTestCase {

  void testBasicFunctionality() {
    assertScript '''
      import groovy.transform.CompileStatic
      import transform.Fluent

      @CompileStatic
      @Fluent
      class Foo {
        int num
        String myName
      }

      def foo = new Foo().setNum(3).setMyName('bar')
      assert foo.num == 3
      assert foo.myName == 'bar'

    '''
  }

}



