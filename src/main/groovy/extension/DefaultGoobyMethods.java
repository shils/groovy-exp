package extension;


import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FirstParam;
import groovy.transform.stc.SecondParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultGoobyMethods {

  public static <T> List<T> findAllWith(@DelegatesTo.Target Collection<T> self, @DelegatesTo(genericTypeIndex = 0) Closure<Boolean> closure) {
    List<T> result = new ArrayList<T>();
    for (T t: self) {
      Closure<Boolean> cloned = (Closure<Boolean>) closure.clone();
      cloned.setDelegate(t);
      cloned.setResolveStrategy(Closure.DELEGATE_FIRST);
      if (cloned.call()) {
        result.add(t);
      }
    }
    return result;
  }

}
