package org.cru.redegg.qualifier;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that the annotated bean will be the one used at runtime.
 *
 * @author Matt Drees
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Selected
{
}
