package exception;

import java.util.Map;

public class SnaggedException extends RuntimeException {

  private Map<String, String> methodArgs;

  public SnaggedException(Map methodArgs, Throwable caughtException) {
    super(caughtException);
    this.methodArgs = methodArgs;
  }

  public Map<String, String> getMethodArgs(){
    return methodArgs;
  }
}
