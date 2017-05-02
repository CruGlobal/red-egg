package org.cru.redegg.reporting.errbit;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A qualifier that mainly just prevents a class from getting the @Default qualifier;
 * that is, the class by itself is not intended to be injected.
 * Instead, the @Default qualifier should be used by some producer method that populates the bean.
 *
 * @author Matt Drees
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Empty
{
}
