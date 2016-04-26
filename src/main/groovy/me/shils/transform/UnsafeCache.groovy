package me.shils.transform

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @author Shil Sinha
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
@GroovyASTTransformationClass('me.shils.internal.transform.UnsafeCacheASTTransformation')
@interface UnsafeCache {

  /**
   * List of names of fields and/or properties which should invalidate the cache when set
   */
  String[] value()
}