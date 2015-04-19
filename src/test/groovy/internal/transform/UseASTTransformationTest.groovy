package internal.transform

import org.codehaus.groovy.control.CompilationFailedException

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

  void testAnnotatedMethodOverridesClass() {
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

        @Use(FakeTimeCategory.class)
        TimeDuration doubleMinutes(Integer t) {
          t.getMinutes()
        }
      }

      class FakeTimeCategory {
        static TimeDuration getMinutes(Integer t){
          TimeCategory.getMinutes(2*t)
        }
      }

      A a  = new A()
      assert a.minutes(3) == TimeCategory.getMinutes(3)
      assert a.doubleMinutes(3) == FakeTimeCategory.getMinutes(3)
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

  void testUsingStaticClass() {
    assertScript '''
      import transform.Use
      import groovy.transform.CompileStatic

      @CompileStatic
      class A {

        @Use(IntCat.class)
        int multiply(int a, int b){
          a.times(b)
        }

        static class IntCat {
          static int times(int a, int b) {
            a*b
          }
        }

      }

      assert new A().multiply(3,4) == A.IntCat.times(3,4)
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

  void testFailOnNonCategoryClass() {
    shouldFail(CompilationFailedException, '''
      import transform.Use
      @Use(B)
      class A {}

      class B {
        void instanceMethod(){}
      }
    ''')
  }

  void testMethodOverriding() {
    assertScript '''
      import transform.Use
      import groovy.transform.CompileStatic

      @CompileStatic
      class A {

        @Use(value = B.class, override = true)
        char charAtOverridden(String text, int index){
          text.charAt(index)
        }

        @Use(value = B.class)
        char charAtNormal(String text, int index) {
          text.charAt(index)
        }

        static class B {
          static char charAt(String self, int index) {
            self[-index - 1] as char
          }
        }
      }

      A a = new A()
      assert a.charAtNormal('abc', 0) == 'abc'.charAt(0)
      assert a.charAtOverridden('abc', 0) == A.B.charAt('abc', 0)
    '''
  }

  void testPropertyOverriding(){
    assertScript '''
      import transform.Use
      import groovy.transform.CompileStatic

      @CompileStatic
      class A {

        @Use(value = B.class, override = true)
        long timeOverridden(Date date){
          date.time
        }

        @Use(value = B.class)
        long timeNormal(Date date) {
          date.time
        }

        static class B {
          static long getTime(Date date) {
            date.next().time
          }
        }
      }

      A a = new A()
      Date now = new Date()
      assert a.timeNormal(now) == now.time
      assert a.timeOverridden(now) == A.B.getTime(now)
    '''

  }
}
