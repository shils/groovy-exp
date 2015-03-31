package internal.transform


class UseASTTransformationTest extends GroovyTestCase {

  void test1() {
    assertScript '''
      import transform.Use

      class B {

        static String blah(Date d){
          'Hello'
        }
      }

      class A {

        @Use(B.class)
        String a1(){
          Date d = new Date()
          d.blah()
        }
      }

      assert new A().a1() == 'Hello'
    '''

  }


}
