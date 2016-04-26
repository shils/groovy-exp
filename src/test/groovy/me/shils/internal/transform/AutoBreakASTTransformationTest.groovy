package me.shils.internal.transform


class AutoBreakASTTransformationTest extends GroovyTestCase {

  void testNoFallThroughAfterNonEmptyCaseStatement() {
    assertScript '''
      @me.shils.transform.AutoBreak
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
      @me.shils.transform.AutoBreak
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
      @me.shils.transform.AutoBreak(includeEmptyCases = true)
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
      @me.shils.transform.AutoBreak
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
      @me.shils.transform.AutoBreak(includeEmptyCases = true)
      class Foo {

        @me.shils.transform.AutoBreak
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
      @me.shils.transform.AutoBreak
      class Foo {

        @me.shils.transform.AutoBreak(includeEmptyCases = true)
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
      @me.shils.transform.AutoBreak(includeEmptyCases = true)
      class Foo {

        @me.shils.transform.AutoBreak
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
      @me.shils.transform.AutoBreak
      class Foo {

        @me.shils.transform.AutoBreak(includeEmptyCases = true)
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
      @me.shils.transform.AutoBreak
      class Foo {

        @me.shils.transform.AutoBreak(includeEmptyCases = true)
        static class Bar {

          @me.shils.transform.AutoBreak
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
      @me.shils.transform.AutoBreak(includeEmptyCases = true)
      class Foo {

        @me.shils.transform.AutoBreak
        static class Bar {

          @me.shils.transform.AutoBreak(includeEmptyCases = true)
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