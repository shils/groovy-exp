package me.shils.transform

import org.codehaus.groovy.transform.GroovyASTTransformationClass
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Local variable level annotation used to emulate C style local static variables.
 *
 * @author Shil Sinha
 */
@Target(ElementType.LOCAL_VARIABLE)
@Retention(RetentionPolicy.SOURCE)
@GroovyASTTransformationClass('me.shils.internal.transform.LocalStaticASTTransformation')
@interface LocalStatic {
}