package internal.transform


class AutoBreakASTTransformationTest extends GroovyTestCase {

  void testNoFallThroughAfterNonEmptyCaseStatement() {
    assertScript '''
      @transform.AutoBreak
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

  void testFallThroughAfterEmptyCaseStatement() {
    assertScript '''
      @transform.AutoBreak
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

  void testAutoBreakOnClass() {
    assertScript '''
      @transform.AutoBreak
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
}
