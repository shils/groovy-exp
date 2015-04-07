package internal.transform

class UseASTTransformationTest extends GroovyTestCase {

  void testAnnotatedMethod() {
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

  void testAnnotatedClass() {
    assertScript '''
      import transform.Use
      import groovy.time.TimeCategory
      import groovy.time.TimeDuration
      import groovy.transform.CompileStatic

      @CompileStatic
      @Use(TimeCategory.class)
      class A {

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

  void testPrimitiveTypeCompatibility() {
    assertScript '''
      import groovy.transform.CompileStatic
      import transform.Use

      class PrimitiveCat {
        static int times(int a, int b) {
          a*b
        }
      }

      class WrapperCat {
        static Integer times(Integer a, Integer b) {
          a*b
        }
      }

      @CompileStatic
      class B {

        @Use(PrimitiveCat)
        int multiplyObject(Integer a, Integer b){
          a.times(b)
        }

        @Use(WrapperCat)
        int multiplyPrimitive(int a, int b) {
          a.times(b)
        }

      }
      B b = new B()
      assert b.multiplyObject(3,4) == 12
      assert b.multiplyPrimitive(3,4) == 12

    '''
  }
}
