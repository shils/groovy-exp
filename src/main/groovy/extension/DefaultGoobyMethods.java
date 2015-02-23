package extension;


import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SecondParam;

class DefaultGoobyMethods {

  public static <T, U extends T, V> V ifInstance(T self, Class<U> clazz, @ClosureParams(SecondParam.FirstGenericType.class) Closure<V> closure) {
    if (clazz.isInstance(self)) {
      U cast = (U) self;
      return closure.call(cast);
    }
    return null;
  }

}
