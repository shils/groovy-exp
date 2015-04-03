package internal.transform

import groovy.transform.NotYetImplemented


class UseASTTransformationTest extends GroovyTestCase {

  void test1() {
    assertScript '''
      import transform.Use
      import groovy.time.TimeCategory
      import groovy.time.TimeDuration
      import groovy.transform.CompileStatic

      @CompileStatic
      class A {

        @Use(TimeCategory.class)
        TimeDuration minutes(Integer t){
          t.getMinutes()
        }
      }

      assert new A().minutes(3) == TimeCategory.getMinutes(3)
    '''
  }

  void testPropertyAccess() {
    assertScript '''
      import transform.Use
      import groovy.time.TimeCategory
      import groovy.time.TimeDuration
      import groovy.transform.CompileStatic

      @CompileStatic
      class A {

        @Use(TimeCategory.class)
        TimeDuration minutes(Integer t){
          t.minutes
        }
      }

      assert new A().minutes(3) == TimeCategory.getMinutes(3)
    '''
  }


}
