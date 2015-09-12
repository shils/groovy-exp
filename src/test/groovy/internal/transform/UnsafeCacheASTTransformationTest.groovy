package internal.transform

class UnsafeCacheASTTransformationTest extends GroovyTestCase {

  void testCaching() {
    assertScript '''
      import transform.UnsafeCache

      class A {

        int x
        String y
        Date z

        @UnsafeCache(['y', 'z'])
        String toString() {
          "y = $y, z = $z".toString()
        }
      }

      A a = new A(y: 'pls')
      String firstCall = a.toString()
      assert firstCall.is(a.toString())

      a.x = 1
      assert firstCall.is(a.toString())

      a.z = new Date()
      String afterInitZ = a.toString()
      assert afterInitZ != firstCall

      a.z = a.z.plus(1)
      String afterSetZ = a.toString()
      assert afterSetZ != afterInitZ
    '''
  }
}