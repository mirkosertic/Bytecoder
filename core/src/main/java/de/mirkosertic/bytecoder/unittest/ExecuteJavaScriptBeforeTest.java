package de.mirkosertic.bytecoder.unittest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExecuteJavaScriptBeforeTest {

  /**
   * @return JS-Code which should be executed before the test runs
   */
  String value();
}
