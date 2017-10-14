package me.shils.util.logging

class EnumConstructorLoggingTest extends GroovyShellTestCase {
  void testJavaLogging() {
    assertScript '''
      @me.shils.util.logging.Log
      enum Pls {
        X('x'), Y('y')
        
        String val
        
        Pls(String val) {
          this.val = val
          log.info("${name()}.val = $val".toString())          
        }
      }
      Pls.X
    '''
  }

  void testJavaLoggingSC() {
    assertScript '''
      @me.shils.util.logging.Log
      @groovy.transform.CompileStatic
      enum Pls {
        X('x'), Y('y')
        
        String val
        
        Pls(String val) {
          this.val = val
          log.info("${name()}.val = $val".toString())          
        }
      }
      Pls.X
    '''
  }

}
