package org.cru.redegg.qualifier;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Matt Drees
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntityStreamPreservation
{
}
