package org.cru.redegg.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * A documentation annotation that indicates that the annotated constructor only exists to make the class proxyable.
 * This is required for all normal-scoped beans in CDI 1.0, unfortunately,
 * even if all injection sites use proxyable types.
 *
 * CDI 1.1 only requires that the type used at the injection target be proxyable.
 */
@Target(ElementType.CONSTRUCTOR)
public @interface ProxyConstructor
{
}
